package com.desconectado.app.data.points

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.util.Date

class PointExpirationProcessor(
    private val firestore: FirebaseFirestore,
    private val now: () -> Instant = Instant::now,
) {
    suspend fun process(uid: String) {
        val user = firestore.collection("users").document(uid)
        if (user.collection("pendingRedemptions").document("current").get(Source.SERVER).await().exists()) return
        val lots = user.collection("pointLots")

        while (true) {
            val candidates = lots
                .whereGreaterThan("remainingPoints", 0)
                .whereLessThanOrEqualTo("expiresAt", Timestamp(Date.from(now())))
                .orderBy("remainingPoints")
                .orderBy("expiresAt")
                .limit(BATCH_SIZE.toLong())
                .get(Source.SERVER)
                .await()
                .documents
            if (candidates.isEmpty()) return

            for (candidate in candidates) {
                try {
                    expire(user, candidate.reference, candidate.id)
                } catch (e: CancellationException) {
                    throw e
                }
            }
            if (candidates.size < BATCH_SIZE) return
        }
    }

    private suspend fun expire(
        user: com.google.firebase.firestore.DocumentReference,
        lot: com.google.firebase.firestore.DocumentReference,
        lotId: String,
    ) {
        val movement = user.collection("movements").document("expire-$lotId")
        firestore.runTransaction { transaction ->
            if (transaction.get(movement).exists()) return@runTransaction Unit
            val lotSnapshot = transaction.get(lot)
            val remaining = lotSnapshot.getLong("remainingPoints") ?: 0L
            val expiresAt = lotSnapshot.getTimestamp("expiresAt")?.toDate()?.toInstant()
            if (!lotSnapshot.exists() || remaining <= 0L || expiresAt == null || now().isBefore(expiresAt)) {
                return@runTransaction Unit
            }
            val userSnapshot = transaction.get(user)
            val balance = userSnapshot.getLong("pointsBalance") ?: 0L
            require(balance >= remaining)

            transaction.update(lot, "remainingPoints", 0L)
            transaction.set(movement, mapOf(
                "type" to "expire",
                "amount" to remaining,
                "lotId" to lotId,
                "sourceId" to lotId,
                "createdAt" to FieldValue.serverTimestamp(),
            ))
            transaction.update(user, mapOf(
                "pointsBalance" to balance - remaining,
                "lastMovementId" to movement.id,
            ))
            Unit
        }.await()
    }

    private companion object {
        const val BATCH_SIZE = 100
    }
}
