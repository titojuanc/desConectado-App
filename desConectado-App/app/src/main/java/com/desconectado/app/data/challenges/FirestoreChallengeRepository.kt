package com.desconectado.app.data.challenges

import com.desconectado.app.data.aErrorApp
import com.desconectado.app.data.aErrorFirestore
import com.desconectado.app.domain.model.ActiveChallenge
import com.desconectado.app.domain.model.ChallengeResult
import com.desconectado.app.domain.model.ChallengeRating
import com.desconectado.app.domain.model.Dificultad
import com.desconectado.app.domain.model.Desafio
import com.desconectado.app.domain.model.DesafioHecho
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.repository.ChallengeRepository
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.desconectado.app.domain.desafioAnteriorAlReset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import android.util.Log
import java.time.Instant

class FirestoreChallengeRepository(
    private val firestore: FirebaseFirestore,
    private val store: ActiveChallengeStore? = null,
) : ChallengeRepository {
    override suspend fun rating(uid: String, runId: String): Resultado<ChallengeRating?> = try {
        val snapshot = firestore.collection("users").document(uid).collection("challengeRatings")
            .document(runId).get(Source.SERVER).await()
        Resultado.Exito(snapshot.toChallengeRating())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resultado.Fallo(e.aErrorApp())
    }

    override suspend fun rate(uid: String, rating: ChallengeRating): Resultado<Unit> = try {
        val user = firestore.collection("users").document(uid)
        val result = user.collection("challengeResults").document(rating.challengeRunId)
        val ratingRef = user.collection("challengeRatings").document(rating.challengeRunId)
        firestore.runTransaction { transaction ->
            val existing = transaction.get(ratingRef)
            if (existing.exists()) {
                require(existing.getLong("stars") == rating.stars.toLong())
                return@runTransaction Unit
            }
            val resultSnapshot = transaction.get(result)
            require(resultSnapshot.exists() && resultSnapshot.getString("status") == ChallengeResult.Status.COMPLETED.name)
            transaction.set(ratingRef, mapOf(
                "challengeRunId" to rating.challengeRunId,
                "stars" to rating.stars,
                "createdAt" to FieldValue.serverTimestamp(),
            ))
            Unit
        }.await()
        Resultado.Exito(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resultado.Fallo(e.aErrorApp())
    }

    override suspend fun history(uid: String): Resultado<List<DesafioHecho>> = try {
        val documents = firestore.collection("users").document(uid).collection("challengeResults")
            .orderBy("finishedAt", Query.Direction.DESCENDING)
            .get(Source.SERVER)
            .await()
            .documents
        Resultado.Exito(documents.mapNotNull { it.toDesafioHecho() })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resultado.Fallo(e.aErrorApp())
    }

    override suspend fun results(uid: String): Resultado<List<ChallengeResult>> = try {
        val documents = firestore.collection("users").document(uid).collection("challengeResults")
            .orderBy("finishedAt", Query.Direction.DESCENDING)
            .get(Source.SERVER)
            .await()
            .documents
        Resultado.Exito(documents.mapNotNull { it.toChallengeResult() })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resultado.Fallo(e.aErrorApp())
    }

    override suspend fun active(uid: String): Resultado<ActiveChallenge?> = try {
        val activeRef = activeReference(uid)
        val user = firestore.collection("users").document(uid)
        val resetAt = user.get(Source.SERVER).await()
            .getTimestamp("delivery3ResetAt")?.toDate()?.toInstant()
        val active = activeRef.get(Source.SERVER).await().toActiveChallenge()
        val local = store?.read()
        if (resetAt != null && active != null && desafioAnteriorAlReset(active.startedAt, resetAt)) {
            activeRef.delete().await()
        }
        if (resetAt != null && local != null && desafioAnteriorAlReset(local.startedAt, resetAt)) {
            store.clear()
        }
        val current = active?.takeUnless { resetAt != null && desafioAnteriorAlReset(it.startedAt, resetAt) }
        if (current != null) store?.write(current)
        Resultado.Exito(current)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resultado.Fallo(e.aErrorApp())
    }

    override suspend fun start(uid: String, challengeId: String): Resultado<ActiveChallenge> = try {
        val activeRef = activeReference(uid)
        val existing = activeRef.get(Source.SERVER).await().toActiveChallenge()
        if (existing != null) return Resultado.Exito(existing)
        val challengeRef = firestore.collection("challenges").document(challengeId)
        firestore.runTransaction { transaction ->
            if (transaction.get(activeRef).exists()) return@runTransaction Unit
            val challenge = transaction.get(challengeRef)
            check(challenge.exists()) { "challenge does not exist" }
            val now = FieldValue.serverTimestamp()
            transaction.set(activeRef, mapOf(
                "challengeId" to challengeId,
                "challengeTitle" to challenge.getString("title").orEmpty(),
                "durationMinutes" to (challenge.getLong("durationMinutes") ?: 0L),
                "points" to (challenge.getLong("points") ?: 0L),
                "category" to challenge.getString("category"),
                "durationSeconds" to (challenge.getLong("durationSeconds") ?: (challenge.getLong("durationMinutes") ?: 0L) * 60L),
                "startedAt" to now,
                "offlineSeconds" to 0L,
                "status" to ActiveChallenge.Status.ACTIVE.name,
                "updatedAt" to now,
            ))
            Unit
        }.await()
        active(uid).valorOrThrow()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "No se pudo iniciar el desafío en Firestore", e)
        Resultado.Fallo(e.aErrorFirestore())
    }

    override suspend fun updateOffline(uid: String, runId: String, seconds: Long): Resultado<Unit> = try {
        activeReference(uid).update(mapOf("offlineSeconds" to seconds.coerceAtLeast(0L), "updatedAt" to FieldValue.serverTimestamp())).await()
        Resultado.Exito(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resultado.Fallo(e.aErrorApp())
    }

    override suspend fun finish(uid: String, runId: String, result: ChallengeResult): Resultado<ChallengeResult> = close(uid, result)

    override suspend fun cancel(uid: String, runId: String): Resultado<ChallengeResult> = close(uid, terminalResult(uid, runId, ChallengeResult.Status.CANCELLED))

    override suspend fun invalidate(uid: String, runId: String, reason: String): Resultado<ChallengeResult> = close(uid, terminalResult(uid, runId, ChallengeResult.Status.INVALIDATED))

    private suspend fun close(uid: String, result: ChallengeResult): Resultado<ChallengeResult> = try {
        val resultRef = firestore.collection("users").document(uid).collection("challengeResults").document(result.challengeRunId)
        val activeRef = activeReference(uid)
        firestore.runTransaction { transaction ->
            val existingResult = transaction.get(resultRef)
            if (existingResult.exists()) {
                return@runTransaction existingResult.toChallengeResult() ?: result
            }
            val activeChallenge = transaction.get(activeRef)
            val finalResult = if (
                result.status == ChallengeResult.Status.CANCELLED ||
                result.status == ChallengeResult.Status.INVALIDATED
            ) {
                check(activeChallenge.exists()) { "active challenge does not exist" }
                result.copy(
                    challengeId = activeChallenge.getString("challengeId").orEmpty(),
                    challengeTitle = activeChallenge.getString("challengeTitle").orEmpty(),
                    durationMinutes = activeChallenge.getLong("durationMinutes")?.toInt() ?: 0,
                    durationSeconds = activeChallenge.getLong("durationSeconds")?.toInt()
                        ?: ((activeChallenge.getLong("durationMinutes") ?: 0L) * 60L).toInt(),
                    category = activeChallenge.getString("category")
                        ?.let(com.desconectado.app.domain.model.CategoriaDesafio::desdeAlmacen),
                    startedAt = activeChallenge.getTimestamp("startedAt")?.toDate()?.toInstant() ?: result.startedAt,
                    offlineSeconds = activeChallenge.getLong("offlineSeconds") ?: result.offlineSeconds,
                    pointsAwarded = 0,
                )
            } else {
                result.copy(
                    category = result.category ?: activeChallenge.getString("category")
                        ?.let(com.desconectado.app.domain.model.CategoriaDesafio::desdeAlmacen),
                )
            }
            transaction.set(resultRef, finalResult.toMap())
            transaction.delete(activeRef)
            finalResult
        }.await()
            .let { Resultado.Exito(it) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resultado.Fallo(e.aErrorApp())
    }

    private fun terminalResult(uid: String, runId: String, status: ChallengeResult.Status): ChallengeResult = ChallengeResult(
        challengeRunId = runId,
        challengeId = "",
        challengeTitle = "",
        durationMinutes = 0,
        startedAt = Instant.EPOCH,
        finishedAt = Instant.now(),
        status = status,
        measuredSocialSeconds = 0,
        offlineSeconds = 0,
        pointsAwarded = 0,
    )

    private fun activeReference(uid: String): DocumentReference = firestore.collection("users").document(uid).collection("activeChallenge").document("current")

    private fun Map<String, Any>.toActiveChallenge(): ActiveChallenge = ActiveChallenge(
        challengeId = get("challengeId") as String,
        challengeTitle = get("challengeTitle") as String,
        durationMinutes = (get("durationMinutes") as Number).toInt(),
        points = (get("points") as Number).toInt(),
        startedAt = (get("startedAt") as com.google.firebase.Timestamp).toDate().toInstant(),
        offlineSeconds = (get("offlineSeconds") as Number).toLong(),
        status = ActiveChallenge.Status.valueOf(get("status") as String),
        updatedAt = (get("updatedAt") as com.google.firebase.Timestamp).toDate().toInstant(),
        durationSeconds = (get("durationSeconds") as? Number)?.toInt() ?: (get("durationMinutes") as Number).toInt() * 60,
        category = (get("category") as? String)
            ?.let(com.desconectado.app.domain.model.CategoriaDesafio::desdeAlmacen),
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toActiveChallenge(): ActiveChallenge? = if (!exists()) null else data?.toActiveChallenge()

    private fun com.google.firebase.firestore.DocumentSnapshot.toDesafioHecho(): DesafioHecho? {
        val titulo = getString("challengeTitle")?.takeIf { it.isNotBlank() } ?: return null
        val estado = getString("status")?.let { runCatching { ChallengeResult.Status.valueOf(it) }.getOrNull() } ?: return null
        val puntos = getLong("pointsAwarded")?.takeIf { it >= 0L && it <= Int.MAX_VALUE }?.toInt() ?: return null
        val fecha = getTimestamp("finishedAt")?.toDate()?.toInstant() ?: return null
        return DesafioHecho(titulo, puntos, fecha, estado)
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toChallengeRating(): ChallengeRating? {
        if (!exists()) return null
        val stars = getLong("stars")?.toInt() ?: return null
        return runCatching {
            ChallengeRating(
                challengeRunId = getString("challengeRunId") ?: id,
                stars = stars,
                createdAt = getTimestamp("createdAt")?.toDate()?.toInstant(),
            )
        }.getOrNull()
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toChallengeResult(): ChallengeResult? = try {
        ChallengeResult(
            challengeRunId = getString("challengeRunId") ?: id,
            challengeId = getString("challengeId") ?: return null,
            challengeTitle = getString("challengeTitle") ?: return null,
            durationMinutes = getLong("durationMinutes")?.toInt() ?: return null,
            durationSeconds = getLong("durationSeconds")?.toInt() ?: return null,
            startedAt = getTimestamp("startedAt")?.toDate()?.toInstant() ?: return null,
            finishedAt = getTimestamp("finishedAt")?.toDate()?.toInstant() ?: return null,
            status = ChallengeResult.Status.valueOf(getString("status") ?: return null),
            measuredSocialSeconds = getLong("measuredSocialSeconds") ?: return null,
            offlineSeconds = getLong("offlineSeconds") ?: return null,
            pointsAwarded = getLong("pointsAwarded")?.toInt() ?: return null,
            timeZoneId = getString("timeZoneId") ?: java.time.ZoneId.systemDefault().id,
            category = getString("category")
                ?.let(com.desconectado.app.domain.model.CategoriaDesafio::desdeAlmacen),
        )
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun ChallengeResult.toMap(): Map<String, Any> = buildMap {
        put("challengeRunId", challengeRunId)
        put("challengeId", challengeId)
        put("challengeTitle", challengeTitle)
        put("durationMinutes", durationMinutes)
        put("durationSeconds", durationSeconds)
        put("startedAt", com.google.firebase.Timestamp(startedAt.epochSecond, startedAt.nano))
        put("finishedAt", com.google.firebase.Timestamp(finishedAt.epochSecond, finishedAt.nano))
        put("status", status.name)
        put("measuredSocialSeconds", measuredSocialSeconds)
        put("offlineSeconds", offlineSeconds)
        put("pointsAwarded", pointsAwarded)
        put("timeZoneId", timeZoneId)
        category?.let { put("category", it.valorAlmacen) }
    }

    private suspend fun Resultado<ActiveChallenge?>.valorOrThrow(): Resultado<ActiveChallenge> = when (this) {
        is Resultado.Exito -> valor?.let { Resultado.Exito(it) } ?: Resultado.Fallo(com.desconectado.app.domain.model.ErrorApp.Desconocido)
        is Resultado.Fallo -> this
    }

    private companion object {
        const val TAG = "FirestoreChallenge"
    }
}
