package com.desconectado.app.data.points

import com.desconectado.app.data.aErrorFirestore
import com.desconectado.app.domain.model.DesafioHecho
import com.desconectado.app.domain.model.ChallengeResult
import com.desconectado.app.domain.model.Recompensa
import com.desconectado.app.domain.model.RedeemedReward
import com.desconectado.app.domain.model.PointLot
import com.desconectado.app.domain.model.CosmeticOwnership
import com.desconectado.app.domain.model.PendingRedemption
import com.desconectado.app.domain.model.UpcomingPointExpiry
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.model.TipoRecompensa
import com.desconectado.app.domain.codigoCupon
import com.desconectado.app.domain.esCosmetico
import com.desconectado.app.domain.seleccionarPremioSorpresa
import com.desconectado.app.domain.proximoVencimiento
import com.desconectado.app.domain.repository.PointsRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.FieldValue
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId

/**
 * Lee los movimientos de `users/{uid}/movements` (contracts/firestore-data.md). La consulta filtra
 * por `type` y ordena por `createdAt`, por eso necesita el índice de `firestore.indexes.json`.
 * Siempre lee del servidor (research D-7).
 */
class FirestorePointsRepository(private val firestore: FirebaseFirestore) : PointsRepository {

    private class SaldoInsuficienteException : IllegalStateException()
    private class OtroCanjePendienteException : IllegalStateException()
    private class RecompensaNoDisponibleException : IllegalStateException()
    private data class InicioCanje(val pending: PendingRedemption?, val redeemed: RedeemedReward?)
    private val expirationProcessor = PointExpirationProcessor(firestore)

    override suspend fun ultimosDesafiosHechos(uid: String, limite: Int): Resultado<List<DesafioHecho>> = try {
        val consulta = firestore.collection("users").document(uid).collection("movements")
            .whereEqualTo("type", TIPO_ACREDITACION)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limite.toLong())
        val documentos = consulta.get(Source.SERVER).await().documents
        Resultado.Exito(documentos.mapNotNull { it.aDesafioHecho() })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "No se pudo leer el saldo", e)
        Resultado.Fallo(e.aErrorFirestore())
    }

    override suspend fun saldo(uid: String): Resultado<Int> = try {
        expirationProcessor.process(uid)
        val value = firestore.collection("users").document(uid).get(Source.SERVER).await().getLong("pointsBalance") ?: 0L
        Resultado.Exito(value.coerceAtLeast(0L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "No se pudo acreditar el desafío", e)
        Resultado.Fallo(e.aErrorFirestore())
    }

    override suspend fun proximoVencimiento(uid: String): Resultado<UpcomingPointExpiry?> = try {
        expirationProcessor.process(uid)
        val documents = firestore.collection("users").document(uid).collection("pointLots")
            .whereGreaterThan("remainingPoints", 0)
            .orderBy("remainingPoints", Query.Direction.ASCENDING)
            .orderBy("expiresAt", Query.Direction.ASCENDING)
            .get(Source.SERVER)
            .await()
            .documents
        Resultado.Exito(proximoVencimiento(documents.mapNotNull { it.toPointLot() }, java.time.Instant.now()))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resultado.Fallo(e.aErrorFirestore())
    }

    override suspend fun acreditar(uid: String, result: ChallengeResult): Resultado<Unit> = try {
        require(result.status == ChallengeResult.Status.COMPLETED)
        val user = firestore.collection("users").document(uid)
        val movement = user.collection("movements").document("credit-${result.challengeRunId}")
        val session = user.collection("pointLotSessions").document("current")
        firestore.runTransaction { transaction ->
            if (transaction.get(movement).exists()) return@runTransaction Unit
            val userSnapshot = transaction.get(user)
            val sessionSnapshot = transaction.get(session)
            val lotId = sessionSnapshot.getString("lotId")
            val lotRef = lotId?.let { user.collection("pointLots").document(it) }
            val activeLot = lotRef?.let { transaction.get(it).toPointLot() }
                ?.takeIf { result.finishedAt >= it.windowStartsAt && result.finishedAt < it.windowEndsAt }
            val lot = activeLot?.agregar(result.pointsAwarded, result.finishedAt)
                ?: PointLot.crear(result.pointsAwarded, result.finishedAt, ZoneId.of(result.timeZoneId))
            val finalLotRef = user.collection("pointLots").document(lot.lotId)
            val current = userSnapshot.getLong("pointsBalance") ?: 0L

            transaction.set(finalLotRef, lot.toFirestoreMap())
            transaction.set(session, mapOf(
                "lotId" to lot.lotId,
                "windowEndsAt" to com.google.firebase.Timestamp(lot.windowEndsAt.epochSecond, lot.windowEndsAt.nano),
            ))
            transaction.set(movement, mapOf(
                "type" to "credit",
                "amount" to result.pointsAwarded,
                "challengeId" to result.challengeId,
                "challengeTitle" to result.challengeTitle,
                "sourceId" to result.challengeRunId,
                "lotId" to lot.lotId,
                "earnedAt" to com.google.firebase.Timestamp(result.finishedAt.epochSecond, result.finishedAt.nano),
                "createdAt" to FieldValue.serverTimestamp(),
            ))
            transaction.update(user, mapOf(
                "pointsBalance" to current + result.pointsAwarded,
                "lastMovementId" to movement.id,
            ))
            Unit
        }.await()
        Resultado.Exito(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resultado.Fallo(e.aErrorFirestore())
    }

    override suspend fun recompensasCanjeadas(uid: String): Resultado<List<RedeemedReward>> = try {
        val docs = firestore.collection("users").document(uid).collection("redeemedRewards")
            .orderBy("createdAt", Query.Direction.DESCENDING).get(Source.SERVER).await().documents
        Resultado.Exito(docs.mapNotNull { it.toRedeemedReward() })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "No se pudieron leer las recompensas canjeadas", e)
        Resultado.Fallo(e.aErrorFirestore())
    }

    override suspend fun pendingRedemption(uid: String): Resultado<PendingRedemption?> = try {
        val snapshot = firestore.collection("users").document(uid)
            .collection("pendingRedemptions").document("current")
            .get(Source.SERVER).await()
        Resultado.Exito(snapshot.toPendingRedemption())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resultado.Fallo(e.aErrorFirestore())
    }

    override suspend fun resumePendingRedemption(uid: String): Resultado<RedeemedReward?> = try {
        when (val loaded = pendingRedemption(uid)) {
            is Resultado.Exito -> loaded.valor?.let { Resultado.Exito(completarCanje(uid, it)) } ?: Resultado.Exito(null)
            is Resultado.Fallo -> loaded
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "No se pudo reanudar el canje pendiente uid=$uid", e)
        Resultado.Fallo(e.aErrorFirestore())
    }

    override suspend fun redeem(uid: String, reward: Recompensa, redemptionId: String): Resultado<RedeemedReward> = try {
        expirationProcessor.process(uid)
        val user = firestore.collection("users").document(uid)
        val redemption = user.collection("redeemedRewards").document(redemptionId)
        val pendingRef = user.collection("pendingRedemptions").document("current")
        val rewardRef = firestore.collection("rewards").document(reward.id)
        cargarCanjeFinalizado(uid, redemptionId)?.let { return Resultado.Exito(it) }
        val pendingBefore = pendingRef.get(Source.SERVER).await().toPendingRedemption()
        if (pendingBefore != null) {
            if (pendingBefore.redemptionId != redemptionId || pendingBefore.rewardId != reward.id) {
                throw OtroCanjePendienteException()
            }
            return Resultado.Exito(completarCanje(uid, pendingBefore))
        }
        val premioCaja = if (reward.kind == TipoRecompensa.CAJA_SORPRESA) {
            val ownedIds = user.collection("cosmeticOwnership").get(Source.SERVER).await().documents
                .map { it.id }
                .toSet()
            val catalogo = firestore.collection("rewards").orderBy("order", Query.Direction.ASCENDING)
                .get(Source.SERVER).await().documents.mapNotNull { it.toRecompensa() }
            seleccionarPremioSorpresa(catalogo, ownedIds, redemptionId) ?: throw RecompensaNoDisponibleException()
        } else {
            null
        }
        val code = if (reward.kind == TipoRecompensa.CUPON) codigoCupon(redemptionId) else null
        val start = firestore.runTransaction { transaction ->
            val existingRedemption = transaction.get(redemption)
            if (existingRedemption.exists()) {
                return@runTransaction InicioCanje(null, existingRedemption.toRedeemedReward())
            }
            val existingPending = transaction.get(pendingRef)
            if (existingPending.exists()) {
                val pending = existingPending.toPendingRedemption() ?: throw IllegalStateException("pending redemption is invalid")
                if (pending.redemptionId != redemptionId || pending.rewardId != reward.id) throw OtroCanjePendienteException()
                return@runTransaction InicioCanje(pending, null)
            }
            val userSnapshot = transaction.get(user)
            val rewardSnapshot = transaction.get(rewardRef)
            check(rewardSnapshot.exists()) { "reward does not exist" }
            check(rewardSnapshot.getLong("costPoints") == reward.costPoints.toLong()) { "reward cost changed" }
            check(rewardSnapshot.getString("kind") == reward.kind.valorAlmacen) { "reward kind changed" }
            check(rewardSnapshot.getBoolean("active") != false) { "reward is inactive" }
            check(rewardSnapshot.stringMap("config") == reward.config) { "reward config changed" }
            if (reward.kind.esCosmetico()) {
                val owned = transaction.get(user.collection("cosmeticOwnership").document(reward.id))
                if (owned.exists()) throw RecompensaNoDisponibleException()
            }
            val premioCajaSnapshot = premioCaja?.let { transaction.get(firestore.collection("rewards").document(it.id)) }
            if (premioCaja != null) {
                val owned = transaction.get(user.collection("cosmeticOwnership").document(premioCaja.id))
                if (owned.exists() || premioCajaSnapshot?.getBoolean("active") == false) {
                    throw RecompensaNoDisponibleException()
                }
                check(premioCajaSnapshot?.getString("kind") == premioCaja.kind.valorAlmacen)
                check(premioCajaSnapshot.stringMap("config") == premioCaja.config)
            }
            val balance = userSnapshot.getLong("pointsBalance") ?: 0L
            if (balance < reward.costPoints) throw SaldoInsuficienteException()
            val pending = PendingRedemption(
                redemptionId = redemptionId,
                rewardId = reward.id,
                name = reward.name,
                costPoints = reward.costPoints,
                code = code,
                pointsDebited = 0,
                lotDebits = emptyMap(),
                createdAt = java.time.Instant.now(),
                kind = reward.kind,
                config = reward.config,
                grantedRewardId = premioCaja?.id,
                grantedRewardName = premioCaja?.name,
                grantedRewardKind = premioCaja?.kind,
                grantedRewardConfig = premioCaja?.config.orEmpty(),
            )
            transaction.set(pendingRef, pending.toFirestoreMap())
            InicioCanje(pending, null)
        }.await()
        start.redeemed?.let { return Resultado.Exito(it) }
        val pending = start.pending ?: throw IllegalStateException("pending redemption was not created")
        Resultado.Exito(completarCanje(uid, pending))
    } catch (e: CancellationException) {
        throw e
    } catch (e: SaldoInsuficienteException) {
        Resultado.Fallo(com.desconectado.app.domain.model.ErrorApp.SaldoInsuficiente)
    } catch (e: RecompensaNoDisponibleException) {
        Resultado.Fallo(com.desconectado.app.domain.model.ErrorApp.RecompensaNoDisponible)
    } catch (e: OtroCanjePendienteException) {
        Resultado.Fallo(com.desconectado.app.domain.model.ErrorApp.Desconocido)
    } catch (e: Exception) {
        Log.e(TAG, "No se pudo canjear la recompensa id=${reward.id} redemptionId=$redemptionId", e)
        Resultado.Fallo(e.aErrorFirestore())
    }

    private suspend fun completarCanje(uid: String, inicio: PendingRedemption): RedeemedReward {
        val user = firestore.collection("users").document(uid)
        var pending = inicio
        while (!pending.canFinalize) {
            val candidates = user.collection("pointLots")
                .whereGreaterThan("remainingPoints", 0)
                .get(Source.SERVER)
                .await()
                .documents
                .mapNotNull { it.toPointLot() }
                .filter { it.remainingPoints > 0 && it.lotId !in pending.lotDebits }
                .sortedBy { it.earnedAt }
            val lot = candidates.firstOrNull() ?: throw SaldoInsuficienteException()
            pending = debitarLote(uid, pending, lot.lotId)
                ?: return cargarCanjeFinalizado(uid, pending.redemptionId)
                ?: throw IllegalStateException("pending redemption disappeared before finalization")
        }
        return finalizarCanje(uid, pending)
    }

    private suspend fun debitarLote(uid: String, pending: PendingRedemption, lotId: String): PendingRedemption? {
        val user = firestore.collection("users").document(uid)
        val lot = user.collection("pointLots").document(lotId)
        val pendingRef = user.collection("pendingRedemptions").document("current")
        val movement = user.collection("movements").document("redeem-${pending.redemptionId}-$lotId")
        return firestore.runTransaction { transaction ->
            val movementSnapshot = transaction.get(movement)
            val pendingSnapshot = transaction.get(pendingRef)
            if (!pendingSnapshot.exists()) return@runTransaction null
            val lotSnapshot = transaction.get(lot)
            val userSnapshot = transaction.get(user)
            val currentPending = pendingSnapshot.toPendingRedemption()
                ?: throw IllegalStateException("pending redemption does not exist")
            if (movementSnapshot.exists()) {
                check(lotId in currentPending.lotDebits)
                return@runTransaction currentPending
            }
            if (lotId in currentPending.lotDebits) return@runTransaction currentPending
            val currentLot = lotSnapshot.toPointLot() ?: throw IllegalStateException("point lot does not exist")
            val amount = minOf(currentLot.remainingPoints, currentPending.pointsRemaining)
            require(amount > 0)
            val balance = userSnapshot.getLong("pointsBalance") ?: 0L
            require(balance >= amount)
            val updatedPending = currentPending.registrarDebito(lotId, amount)

            transaction.update(lot, "remainingPoints", currentLot.remainingPoints - amount)
            transaction.set(movement, mapOf(
                "type" to "redeem",
                "amount" to amount,
                "rewardId" to currentPending.rewardId,
                "sourceId" to currentPending.redemptionId,
                "redemptionId" to currentPending.redemptionId,
                "lotId" to lotId,
                "createdAt" to FieldValue.serverTimestamp(),
                "code" to currentPending.code,
            ))
            transaction.update(user, mapOf(
                "pointsBalance" to balance - amount,
                "lastMovementId" to movement.id,
            ))
            transaction.update(pendingRef, mapOf(
                "pointsDebited" to updatedPending.pointsDebited,
                "lotDebits" to updatedPending.lotDebits,
                "updatedAt" to FieldValue.serverTimestamp(),
            ))
            updatedPending
        }.await()
    }

    private suspend fun finalizarCanje(uid: String, pending: PendingRedemption): RedeemedReward {
        val user = firestore.collection("users").document(uid)
        val pendingRef = user.collection("pendingRedemptions").document("current")
        val redemption = user.collection("redeemedRewards").document(pending.redemptionId)
        val movementIds = pending.lotDebits.keys.sorted().map { "redeem-${pending.redemptionId}-$it" }
        val movementId = movementIds.firstOrNull() ?: throw IllegalStateException("pending redemption has no debits")
        val now = java.time.Instant.now()
        return firestore.runTransaction { transaction ->
            val existing = transaction.get(redemption)
            val pendingSnapshot = transaction.get(pendingRef)
            if (existing.exists()) return@runTransaction existing.toRedeemedReward()
                ?: throw IllegalStateException("redeemed reward is invalid")
            val current = pendingSnapshot.toPendingRedemption()
                ?: throw IllegalStateException("pending redemption does not exist")
            check(current.redemptionId == pending.redemptionId && current.canFinalize)
            val datosCanje = mapOf(
                "redemptionId" to current.redemptionId,
                "rewardId" to current.rewardId,
                "name" to current.name,
                "costPoints" to current.costPoints,
                "movementId" to movementId,
                "movementIds" to movementIds,
                "code" to current.code,
                "createdAt" to FieldValue.serverTimestamp(),
                "kind" to current.kind?.valorAlmacen,
                "config" to current.config,
                "grantedRewardId" to current.grantedRewardId,
                "grantedRewardName" to current.grantedRewardName,
                "grantedRewardKind" to current.grantedRewardKind?.valorAlmacen,
                "grantedRewardConfig" to current.grantedRewardConfig,
            )
            transaction.set(redemption, datosCanje)
            cosmeticOwnership(current)?.let { ownership ->
                transaction.set(user.collection("cosmeticOwnership").document(ownership.rewardId), ownership.toFirestoreMap())
            }
            transaction.delete(pendingRef)
            RedeemedReward(
                current.redemptionId,
                current.rewardId,
                current.name,
                current.costPoints,
                movementId,
                current.code,
                now,
                movementIds,
                current.kind,
                current.config,
                current.grantedRewardId,
                current.grantedRewardName,
                current.grantedRewardKind,
                current.grantedRewardConfig,
            )
        }.await()
    }

    private fun cosmeticOwnership(pending: PendingRedemption): CosmeticOwnership? {
        if (pending.kind?.esCosmetico() == true) {
            return CosmeticOwnership(
                rewardId = pending.rewardId,
                name = pending.name,
                kind = pending.kind,
                config = pending.config,
                acquiredAt = pending.createdAt,
                redemptionId = pending.redemptionId,
            )
        }
        val grantedKind = pending.grantedRewardKind
        val grantedId = pending.grantedRewardId
        val grantedName = pending.grantedRewardName
        if (grantedKind?.esCosmetico() != true || grantedId == null || grantedName == null) return null
        return CosmeticOwnership(
            rewardId = grantedId,
            name = grantedName,
            kind = grantedKind,
            config = pending.grantedRewardConfig,
            acquiredAt = pending.createdAt,
            redemptionId = pending.redemptionId,
        )
    }

    private fun CosmeticOwnership.toFirestoreMap(): Map<String, Any> = mapOf(
        "rewardId" to rewardId,
        "name" to name,
        "kind" to kind.valorAlmacen,
        "config" to config,
        "redemptionId" to redemptionId,
        "createdAt" to FieldValue.serverTimestamp(),
    )

    private suspend fun cargarCanjeFinalizado(uid: String, redemptionId: String): RedeemedReward? =
        firestore.collection("users").document(uid).collection("redeemedRewards")
            .document(redemptionId).get(Source.SERVER).await().toRedeemedReward()

    /** Un movimiento incompleto o con valores fuera de rango se descarta en vez de romper la lista. */
    private fun DocumentSnapshot.aDesafioHecho(): DesafioHecho? {
        val titulo = getString("challengeTitle")?.takeIf { it.isNotBlank() } ?: return null
        val monto = getLong("amount")?.takeIf { it in 1..Int.MAX_VALUE } ?: return null
        val fecha = getTimestamp("createdAt")?.toDate()?.toInstant() ?: return null
        return DesafioHecho(titulo = titulo, puntos = monto.toInt(), fecha = fecha)
    }

    private fun DocumentSnapshot.toRedeemedReward(): RedeemedReward? {
        val date = getTimestamp("createdAt")?.toDate()?.toInstant() ?: return null
        val movementId = getString("movementId") ?: return null
        return RedeemedReward(
            redemptionId = getString("redemptionId") ?: id,
            rewardId = getString("rewardId") ?: return null,
            name = getString("name") ?: return null,
            costPoints = getLong("costPoints")?.toInt() ?: return null,
            movementId = movementId,
            code = getString("code"),
            createdAt = date,
            movementIds = (get("movementIds") as? List<*>)?.filterIsInstance<String>()
                ?: listOf(movementId),
            kind = TipoRecompensa.desdeAlmacen(getString("kind")),
            config = stringMap("config"),
            grantedRewardId = getString("grantedRewardId"),
            grantedRewardName = getString("grantedRewardName"),
            grantedRewardKind = TipoRecompensa.desdeAlmacen(getString("grantedRewardKind")),
            grantedRewardConfig = stringMap("grantedRewardConfig"),
        )
    }

    private fun DocumentSnapshot.toPendingRedemption(): PendingRedemption? {
        if (!exists()) return null
        val lotDebits = (get("lotDebits") as? Map<*, *>)
            ?.mapNotNull { (key, value) ->
                val lotId = key as? String ?: return@mapNotNull null
                val amount = (value as? Number)?.toInt() ?: return@mapNotNull null
                lotId to amount
            }
            ?.toMap()
            ?: emptyMap()
        return try {
            PendingRedemption(
                redemptionId = getString("redemptionId") ?: return null,
                rewardId = getString("rewardId") ?: return null,
                name = getString("name") ?: return null,
                costPoints = getLong("costPoints")?.toInt() ?: return null,
                code = getString("code"),
                pointsDebited = getLong("pointsDebited")?.toInt() ?: return null,
                lotDebits = lotDebits,
                createdAt = getTimestamp("createdAt")?.toDate()?.toInstant() ?: return null,
                kind = TipoRecompensa.desdeAlmacen(getString("kind")),
                config = stringMap("config"),
                grantedRewardId = getString("grantedRewardId"),
                grantedRewardName = getString("grantedRewardName"),
                grantedRewardKind = TipoRecompensa.desdeAlmacen(getString("grantedRewardKind")),
                grantedRewardConfig = stringMap("grantedRewardConfig"),
            )
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun PendingRedemption.toFirestoreMap(): Map<String, Any?> = mapOf(
        "redemptionId" to redemptionId,
        "rewardId" to rewardId,
        "name" to name,
        "costPoints" to costPoints,
        "code" to code,
        "pointsDebited" to pointsDebited,
        "lotDebits" to lotDebits,
        "kind" to kind?.valorAlmacen,
        "config" to config,
        "grantedRewardId" to grantedRewardId,
        "grantedRewardName" to grantedRewardName,
        "grantedRewardKind" to grantedRewardKind?.valorAlmacen,
        "grantedRewardConfig" to grantedRewardConfig,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )

    private fun DocumentSnapshot.toPointLot(): PointLot? = try {
        val localDate = getString("localDate")?.let(LocalDate::parse) ?: return null
        PointLot(
            lotId = getString("lotId") ?: id,
            localDate = localDate,
            timeZoneId = getString("timeZoneId") ?: return null,
            windowEndsAt = getTimestamp("windowEndsAt")?.toDate()?.toInstant() ?: return null,
            expiresAt = getTimestamp("expiresAt")?.toDate()?.toInstant() ?: return null,
            earnedAt = getTimestamp("earnedAt")?.toDate()?.toInstant() ?: return null,
            issuedPoints = getLong("issuedPoints")?.toInt() ?: return null,
            remainingPoints = getLong("remainingPoints")?.toInt() ?: return null,
        )
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun PointLot.toFirestoreMap(): Map<String, Any> = mapOf(
        "lotId" to lotId,
        "localDate" to localDate.toString(),
        "timeZoneId" to timeZoneId,
        "windowStartsAt" to com.google.firebase.Timestamp(windowStartsAt.epochSecond, windowStartsAt.nano),
        "windowEndsAt" to com.google.firebase.Timestamp(windowEndsAt.epochSecond, windowEndsAt.nano),
        "earnedAt" to com.google.firebase.Timestamp(earnedAt.epochSecond, earnedAt.nano),
        "expiresAt" to com.google.firebase.Timestamp(expiresAt.epochSecond, expiresAt.nano),
        "issuedPoints" to issuedPoints,
        "remainingPoints" to remainingPoints,
    )

    private fun DocumentSnapshot.toRecompensa(): Recompensa? {
        val kind = TipoRecompensa.desdeAlmacen(getString("kind")) ?: return null
        return Recompensa(
            id = id,
            name = getString("name") ?: return null,
            description = getString("description") ?: return null,
            costPoints = getLong("costPoints")?.toInt() ?: return null,
            kind = kind,
            order = getLong("order")?.toInt() ?: return null,
            active = getBoolean("active") ?: true,
            config = stringMap("config"),
        )
    }

    private fun DocumentSnapshot.stringMap(field: String): Map<String, String> =
        (get(field) as? Map<*, *>)
            ?.mapNotNull { (key, value) ->
                val keyText = key as? String ?: return@mapNotNull null
                val valueText = value as? String ?: return@mapNotNull null
                keyText to valueText
            }
            ?.toMap()
            ?: emptyMap()

    private companion object {
        const val TIPO_ACREDITACION = "credit"
        const val TAG = "FirestorePoints"
    }
}
