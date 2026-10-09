package com.desconectado.app.domain

import com.desconectado.app.domain.model.AchievementCriterion
import com.desconectado.app.domain.model.AchievementDefinition
import com.desconectado.app.domain.model.CategoriaDesafio
import com.desconectado.app.domain.model.ChallengeResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class AchievementCalculatorTest {
    @Test
    fun desbloqueaEnElUmbralYConservaProgresoParcialAntesDelUmbral() {
        val definiciones = listOf(
            definicion("en-marcha", AchievementCriterion.COMPLETED_CHALLENGES, 5),
            definicion("modo-presente", AchievementCriterion.COMPLETED_SECONDS, 18_000),
            definicion("sin-apuro", AchievementCriterion.SINGLE_CHALLENGE_SECONDS, 10_800),
            definicion("aire-libre", AchievementCriterion.CATEGORY_COMPLETIONS, 5, CategoriaDesafio.MOVERME),
        )
        val cuatroResultados = (0 until 4).map { resultado(it, duracion = 3_600) }

        val parcial = AchievementCalculator.calcular(cuatroResultados, definiciones)

        assertEquals(4, parcial.getValue("en-marcha").progress)
        assertFalse(parcial.getValue("en-marcha").unlocked)
        assertEquals(14_400, parcial.getValue("modo-presente").progress)
        assertFalse(parcial.getValue("sin-apuro").unlocked)
        assertEquals(4, parcial.getValue("aire-libre").progress)

        val enUmbral = AchievementCalculator.calcular(
            cuatroResultados + resultado(4, duracion = 3_600),
            definiciones,
        )

        assertTrue(enUmbral.getValue("en-marcha").unlocked)
        assertTrue(enUmbral.getValue("modo-presente").unlocked)
        assertTrue(enUmbral.getValue("aire-libre").unlocked)
        assertFalse(enUmbral.getValue("sin-apuro").unlocked)
    }

    @Test
    fun logroExploradorCuentaCategoriasDistintasYDesafioLargoAlcanzaTresHoras() {
        val definiciones = listOf(
            definicion("sin-apuro", AchievementCriterion.SINGLE_CHALLENGE_SECONDS, 10_800),
            definicion("explorador", AchievementCriterion.EXPLORED_CATEGORIES, 4),
        )
        val resultados = listOf(
            resultado(0, CategoriaDesafio.MOVERME, 10_800),
            resultado(1, CategoriaDesafio.MOVERME),
            resultado(2, CategoriaDesafio.ENFOCARME),
            resultado(3, CategoriaDesafio.SOCIALIZAR),
            resultado(4, CategoriaDesafio.DESCANSAR, estado = ChallengeResult.Status.FAILED),
        )

        val progreso = AchievementCalculator.calcular(resultados, definiciones)

        assertTrue(progreso.getValue("sin-apuro").unlocked)
        assertEquals(3, progreso.getValue("explorador").progress)
        assertFalse(progreso.getValue("explorador").unlocked)
    }

    private fun definicion(
        id: String,
        criterio: AchievementCriterion,
        umbral: Int,
        categoria: CategoriaDesafio? = null,
    ) = AchievementDefinition(id, id, "Descripción", criterio, umbral, 1, categoria)

    private fun resultado(
        indice: Int,
        categoria: CategoriaDesafio = CategoriaDesafio.MOVERME,
        duracion: Int = 1_800,
        estado: ChallengeResult.Status = ChallengeResult.Status.COMPLETED,
    ) = ChallengeResult(
        challengeRunId = "run-$indice",
        challengeId = "challenge-$indice",
        challengeTitle = "Prueba",
        durationMinutes = duracion / 60,
        startedAt = Instant.parse("2026-10-05T00:00:00Z").plusSeconds(indice * 86_400L),
        finishedAt = Instant.parse("2026-10-05T00:00:00Z").plusSeconds(indice * 86_400L + duracion),
        status = estado,
        measuredSocialSeconds = 0,
        offlineSeconds = 0,
        pointsAwarded = if (estado == ChallengeResult.Status.COMPLETED) 10 else 0,
        durationSeconds = duracion,
        timeZoneId = "UTC",
        category = categoria,
    )
}