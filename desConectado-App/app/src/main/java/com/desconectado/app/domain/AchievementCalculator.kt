package com.desconectado.app.domain

import com.desconectado.app.domain.model.AchievementCriterion
import com.desconectado.app.domain.model.AchievementDefinition
import com.desconectado.app.domain.model.AchievementProgress
import com.desconectado.app.domain.model.CategoriaDesafio
import com.desconectado.app.domain.model.ChallengeResult

object AchievementCalculator {
    fun calcular(
        resultados: List<ChallengeResult>,
        definiciones: List<AchievementDefinition>,
    ): Map<String, AchievementProgress> {
        val completados = resultados.filter {
            it.status == ChallengeResult.Status.COMPLETED && it.durationSeconds > 0
        }
        val tiempoTotal = completados.sumOf { it.durationSeconds.toLong() }
        val maximoDesafio = completados.maxOfOrNull { it.durationSeconds.toLong() } ?: 0L
        val categorias = completados.mapNotNull { it.category }

        return definiciones.filter { it.active }.associate { definicion ->
            val progreso = when (definicion.criterion) {
                AchievementCriterion.COMPLETED_CHALLENGES -> completados.size.toLong()
                AchievementCriterion.COMPLETED_SECONDS -> tiempoTotal
                AchievementCriterion.SINGLE_CHALLENGE_SECONDS -> maximoDesafio
                AchievementCriterion.CATEGORY_COMPLETIONS -> categorias.count { it == definicion.category }.toLong()
                AchievementCriterion.EXPLORED_CATEGORIES -> categorias.distinct().size.toLong()
            }.coerceAtMost(definicion.threshold.toLong()).toInt()

            definicion.id to AchievementProgress(definicion, progreso)
        }
    }

    fun conteosPorCategoria(resultados: List<ChallengeResult>): Map<CategoriaDesafio, Int> = resultados
        .asSequence()
        .filter { it.status == ChallengeResult.Status.COMPLETED && it.durationSeconds > 0 }
        .mapNotNull { it.category }
        .groupingBy { it }
        .eachCount()
}