package com.desconectado.app.domain

import com.desconectado.app.domain.model.ChallengeResult
import com.desconectado.app.domain.model.CategoriaDesafio
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class ProgressCalculatorTest {
    @Test
    fun agregaSoloResultadosCompletadosYCalculaElAvanceSemanal() {
        val resultados = listOf(
            resultado("2026-10-06T12:00:00Z", duracionSegundos = 1_800),
            resultado("2026-10-07T12:00:00Z", duracionSegundos = 3_600),
            resultado("2026-10-07T13:00:00Z", estado = ChallengeResult.Status.FAILED, duracionSegundos = 7_200),
            resultado("2026-10-07T14:00:00Z", estado = ChallengeResult.Status.CANCELLED, duracionSegundos = 900),
        )

        val progreso = ProgressCalculator.calcular(
            resultados = resultados,
            ahora = Instant.parse("2026-10-07T15:00:00Z"),
            zonaActual = ZoneId.of("UTC"),
            metaSemanalMinutos = 120,
        )

        assertEquals(2, progreso.desafiosCompletados)
        assertEquals(5_400L, progreso.tiempoCompletadoSegundos)
        assertEquals(2, progreso.rachaDias)
        assertEquals(5_400L, progreso.tiempoSemanalSegundos)
        assertEquals(30, progreso.minutosRestantesMetaSemanal)
        assertEquals(2, progreso.desafiosPorCategoria[CategoriaDesafio.MOVERME])
    }

    @Test
    fun usaLaZonaCapturadaEnCadaResultadoParaDeterminarSuDiaLocal() {
        val instante = "2026-10-05T06:30:00Z"
        val resultados = listOf(
            resultado(instante, zona = "America/Los_Angeles"),
            resultado(instante, zona = "UTC"),
        )

        val progreso = ProgressCalculator.calcular(
            resultados = resultados,
            ahora = Instant.parse("2026-10-05T12:00:00Z"),
            zonaActual = ZoneId.of("UTC"),
            metaSemanalMinutos = 60,
        )

        assertEquals(2, progreso.rachaDias)
    }

    @Test
    fun conservaLaRachaDuranteHoyPeroLaPierdeTrasUnDiaLocalCompletoSinCumplir() {
        val resultados = listOf(resultado("2026-10-05T12:00:00Z"))

        val duranteHoy = ProgressCalculator.calcular(
            resultados = resultados,
            ahora = Instant.parse("2026-10-06T12:00:00Z"),
            zonaActual = ZoneId.of("UTC"),
            metaSemanalMinutos = 60,
        )
        val despuesDelDiaIncumplido = ProgressCalculator.calcular(
            resultados = resultados,
            ahora = Instant.parse("2026-10-07T00:00:00Z"),
            zonaActual = ZoneId.of("UTC"),
            metaSemanalMinutos = 60,
        )

        assertEquals(1, duranteHoy.rachaDias)
        assertEquals(0, despuesDelDiaIncumplido.rachaDias)
    }

    @Test
    fun laSemanaComienzaElLunesYNoIncluyeElDomingoAnterior() {
        val resultados = listOf(
            resultado("2026-10-04T12:00:00Z", duracionSegundos = 3_600),
            resultado("2026-10-05T12:00:00Z", duracionSegundos = 1_800),
        )

        val progreso = ProgressCalculator.calcular(
            resultados = resultados,
            ahora = Instant.parse("2026-10-05T18:00:00Z"),
            zonaActual = ZoneId.of("UTC"),
            metaSemanalMinutos = 60,
        )

        assertEquals(5_400L, progreso.tiempoCompletadoSegundos)
        assertEquals(1_800L, progreso.tiempoSemanalSegundos)
    }

    @Test
    fun noInventaUnaMetaSemanalSiTodaviaNoFueConfigurada() {
        val progreso = ProgressCalculator.calcular(
            resultados = emptyList(),
            ahora = Instant.parse("2026-10-07T15:00:00Z"),
            zonaActual = ZoneId.of("UTC"),
        )

        assertEquals(null, progreso.minutosRestantesMetaSemanal)
        assertEquals(0L, progreso.tiempoSemanalSegundos)
    }

    @Test
    fun noCuentaCategoriaDeResultadosNoCompletados() {
        val resultados = listOf(
            resultado("2026-10-05T12:00:00Z", categoria = CategoriaDesafio.MOVERME),
            resultado(
                "2026-10-06T12:00:00Z",
                estado = ChallengeResult.Status.FAILED,
                categoria = CategoriaDesafio.MOVERME,
            ),
        )

        val progreso = ProgressCalculator.calcular(
            resultados = resultados,
            ahora = Instant.parse("2026-10-07T15:00:00Z"),
            zonaActual = ZoneId.of("UTC"),
            metaSemanalMinutos = 60,
        )

        assertEquals(mapOf(CategoriaDesafio.MOVERME to 1), progreso.desafiosPorCategoria)
    }

    private fun resultado(
        finalizado: String,
        estado: ChallengeResult.Status = ChallengeResult.Status.COMPLETED,
        duracionSegundos: Int = 1_800,
        zona: String = "UTC",
        categoria: CategoriaDesafio = CategoriaDesafio.MOVERME,
    ) = ChallengeResult(
        challengeRunId = finalizado + estado.name + zona,
        challengeId = "challenge",
        challengeTitle = "Prueba",
        durationMinutes = duracionSegundos / 60,
        startedAt = Instant.parse(finalizado).minusSeconds(duracionSegundos.toLong()),
        finishedAt = Instant.parse(finalizado),
        status = estado,
        measuredSocialSeconds = 0,
        offlineSeconds = 0,
        pointsAwarded = if (estado == ChallengeResult.Status.COMPLETED) 10 else 0,
        durationSeconds = duracionSegundos,
        timeZoneId = zona,
        category = categoria,
    )
}