package com.desconectado.app.data.profile

import com.desconectado.app.data.aErrorApp
import com.desconectado.app.domain.AchievementCalculator
import com.desconectado.app.domain.model.AchievementCriterion
import com.desconectado.app.domain.model.AchievementDefinition
import com.desconectado.app.domain.model.AchievementProgress
import com.desconectado.app.domain.model.CategoriaDesafio
import com.desconectado.app.domain.model.ChallengeResult
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.repository.AchievementRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class FirestoreAchievementRepository(private val firestore: FirebaseFirestore) : AchievementRepository {
    override suspend fun actualizar(
        uid: String,
        resultados: List<ChallengeResult>,
    ): Resultado<List<AchievementProgress>> = try {
        val definiciones = firestore.collection("achievements")
            .orderBy("order", Query.Direction.ASCENDING)
            .get(Source.SERVER)
            .await()
            .documents
            .mapNotNull { it.aDefinicion() }
            .filter { it.active }
        val progreso = AchievementCalculator.calcular(resultados, definiciones).values.toList()
        progreso.forEach { logro -> guardarProgreso(uid, logro) }
        Resultado.Exito(progreso)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resultado.Fallo(e.aErrorApp())
    }

    private suspend fun guardarProgreso(uid: String, progreso: AchievementProgress) {
        val referencia = firestore.collection("users").document(uid)
            .collection("achievements").document(progreso.definition.id)
        firestore.runTransaction { transaccion ->
            val existente = transaccion.get(referencia)
            val progresoGuardado = existente.getLong("progress")?.toInt() ?: 0
            val yaDesbloqueado = existente.getTimestamp("unlockedAt") != null
            if (yaDesbloqueado || progreso.progress <= progresoGuardado) return@runTransaction

            val datos = buildMap<String, Any> {
                put("achievementId", progreso.definition.id)
                put("progress", progreso.progress)
                put("threshold", progreso.threshold)
                put("updatedAt", FieldValue.serverTimestamp())
                if (progreso.unlocked) put("unlockedAt", FieldValue.serverTimestamp())
            }
            transaccion.set(referencia, datos)
            Unit
        }.await()
    }

    private fun DocumentSnapshot.aDefinicion(): AchievementDefinition? {
        val criterio = AchievementCriterion.desdeAlmacen(getString("criterion")) ?: return null
        val categoriaAlmacenada = getString("category")
        val categoria = categoriaAlmacenada?.let(CategoriaDesafio::desdeAlmacen)
        val umbral = getLong("threshold")?.takeIf { it in 1..Int.MAX_VALUE }?.toInt() ?: return null
        val orden = getLong("order")?.takeIf { it in 1..Int.MAX_VALUE }?.toInt() ?: return null
        return runCatching {
            AchievementDefinition(
                id = getString("achievementId") ?: id,
                name = getString("name") ?: return null,
                description = getString("description") ?: return null,
                criterion = criterio,
                threshold = umbral,
                order = orden,
                category = categoria,
                active = getBoolean("active") ?: true,
            )
        }.getOrNull()
    }
}