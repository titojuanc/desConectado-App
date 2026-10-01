package com.desconectado.app.data.points

import com.desconectado.app.data.aErrorFirestore
import com.desconectado.app.domain.model.DesafioHecho
import com.desconectado.app.domain.model.ChallengeResult
import com.desconectado.app.domain.model.Recompensa
import com.desconectado.app.domain.model.RedeemedReward
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.model.TipoRecompensa
import com.desconectado.app.domain.codigoCupon
import com.desconectado.app.domain.repository.PointsRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.FieldValue
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * Lee los movimientos de `users/{uid}/movements` (contracts/firestore-data.md). La consulta filtra
 * por `type` y ordena por `createdAt`, por eso necesita el índice de `firestore.indexes.json`.
 * Siempre lee del servidor (research D-7).
 */
class FirestorePointsRepository(private val firestore: FirebaseFirestore) : PointsRepository {

    private class SaldoInsuficienteException : IllegalStateException()

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
        val value = firestore.collection("users").document(uid).get(Source.SERVER).await().getLong("pointsBalance") ?: 0L
        Resultado.Exito(value.coerceAtLeast(0L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "No se pudo acreditar el desafío", e)
        Resultado.Fallo(e.aErrorFirestore())
    }

    override suspend fun acreditar(uid: String, result: ChallengeResult): Resultado<Unit> = try {
        require(result.status == ChallengeResult.Status.COMPLETED)
        val user = firestore.collection("users").document(uid)
        val movement = user.collection("movements").document("credit-${result.challengeRunId}")
        firestore.runTransaction { transaction ->
            if (!transaction.get(movement).exists()) {
                val current = transaction.get(user).getLong("pointsBalance") ?: 0L
                transaction.set(movement, mapOf(
                    "type" to "credit",
                    "amount" to result.pointsAwarded,
                    "challengeId" to result.challengeId,
                    "challengeTitle" to result.challengeTitle,
                    "sourceId" to result.challengeRunId,
                    "createdAt" to FieldValue.serverTimestamp(),
                ))
                transaction.update(user, mapOf(
                    "pointsBalance" to current + result.pointsAwarded,
                    "lastMovementId" to movement.id,
                ))
            }
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

    override suspend fun redeem(uid: String, reward: Recompensa, redemptionId: String): Resultado<RedeemedReward> = try {
        val user = firestore.collection("users").document(uid)
        val redemption = user.collection("redeemedRewards").document(redemptionId)
        val movement = user.collection("movements").document("redeem-$redemptionId")
        val code = if (reward.kind == TipoRecompensa.CUPON) codigoCupon(redemptionId) else null
        val now = java.time.Instant.now()
        firestore.runTransaction { transaction ->
            val existing = transaction.get(redemption)
            if (!existing.exists()) {
                val balance = transaction.get(user).getLong("pointsBalance") ?: 0L
                if (balance < reward.costPoints) throw SaldoInsuficienteException()
                transaction.set(redemption, mapOf(
                    "redemptionId" to redemptionId,
                    "rewardId" to reward.id,
                    "name" to reward.name,
                    "costPoints" to reward.costPoints,
                    "movementId" to movement.id,
                    "code" to code,
                    "createdAt" to FieldValue.serverTimestamp(),
                ))
                transaction.set(movement, mapOf(
                    "type" to "redeem",
                    "amount" to reward.costPoints,
                    "rewardId" to reward.id,
                    "sourceId" to redemptionId,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "code" to code,
                ))
                transaction.update(user, mapOf(
                    "pointsBalance" to balance - reward.costPoints,
                    "lastMovementId" to movement.id,
                ))
            }
            Unit
        }.await()
        Resultado.Exito(RedeemedReward(redemptionId, reward.id, reward.name, reward.costPoints, movement.id, code, now))
    } catch (e: CancellationException) {
        throw e
    } catch (e: SaldoInsuficienteException) {
        Resultado.Fallo(com.desconectado.app.domain.model.ErrorApp.SaldoInsuficiente)
    } catch (e: Exception) {
        Log.e(TAG, "No se pudo canjear la recompensa id=${reward.id} redemptionId=$redemptionId", e)
        Resultado.Fallo(e.aErrorFirestore())
    }

    /** Un movimiento incompleto o con valores fuera de rango se descarta en vez de romper la lista. */
    private fun DocumentSnapshot.aDesafioHecho(): DesafioHecho? {
        val titulo = getString("challengeTitle")?.takeIf { it.isNotBlank() } ?: return null
        val monto = getLong("amount")?.takeIf { it in 1..Int.MAX_VALUE } ?: return null
        val fecha = getTimestamp("createdAt")?.toDate()?.toInstant() ?: return null
        return DesafioHecho(titulo = titulo, puntos = monto.toInt(), fecha = fecha)
    }

    private fun DocumentSnapshot.toRedeemedReward(): RedeemedReward? {
        val date = getTimestamp("createdAt")?.toDate()?.toInstant() ?: return null
        return RedeemedReward(
            redemptionId = getString("redemptionId") ?: id,
            rewardId = getString("rewardId") ?: return null,
            name = getString("name") ?: return null,
            costPoints = getLong("costPoints")?.toInt() ?: return null,
            movementId = getString("movementId") ?: return null,
            code = getString("code"),
            createdAt = date,
        )
    }

    private companion object {
        const val TIPO_ACREDITACION = "credit"
        const val TAG = "FirestorePoints"
    }
}
