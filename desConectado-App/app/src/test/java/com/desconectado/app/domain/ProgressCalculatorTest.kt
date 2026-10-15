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
    fun metaDiariaDeSeisHorasSeAlcanzaConSeisHorasEnElDia() {
        val progreso = ProgressCalculator.calcular(
            resultados = listOf(resultado("2026-10-07T12:00:00Z", duracionSegundos = 21_600)),
            ahora = Instant.parse("2026-10-07T15:00:00Z"),
            zonaActual = ZoneId.of("UTC"),
            metaSemanalMinutos = 2520,
        )

        assertEquals(true, progreso.metaDiariaCumplida)
        assertEquals(0L, progreso.segundosRestantesMetaDiaria)
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

    @Test
    fun aplicaBonoPorRachaYMetaSemanalRedondeandoPuntosHaciaArriba() {
        val historial = (1..6).map { dia ->
            resultado("2026-10-%02dT12:00:00Z".format(dia), duracionSegundos = 7_200)
        }
        val hoy = resultado("2026-10-07T12:00:00Z", duracionSegundos = 7_200)

        val metaModerada = aplicarMultiplicadorRacha(hoy, historial, metaSemanalMinutos = 210)
        val metaAlta = aplicarMultiplicadorRacha(hoy, historial, metaSemanalMinutos = 840)

        assertEquals(1.07, metaModerada.pointsMultiplier, 0.000001)
        assertEquals(11, metaModerada.pointsAwarded)
        assertEquals(1.28, metaAlta.pointsMultiplier, 0.000001)
        assertEquals(13, metaAlta.pointsAwarded)
    }

    @Test
    fun sinMetaConfiguradaMantieneLosPuntosBase() {
        val resultado = resultado("2026-10-07T12:00:00Z")

        val resultadoSinBono = aplicarMultiplicadorRacha(resultado, emptyList(), metaSemanalMinutos = null)

        assertEquals(10, resultadoSinBono.pointsAwarded)
        assertEquals(1.0, resultadoSinBono.pointsMultiplier, 0.0)
    }

    @Test
    fun hoyNoAumentaLaRachaHastaCumplirLaMetaDiariaExacta() {
        val ayer = resultado("2026-10-06T12:00:00Z")
        val hoy = resultado("2026-10-07T12:00:00Z", duracionSegundos = 1_799)
        val pendiente = ProgressCalculator.calcular(
            listOf(ayer, hoy), hoy.finishedAt, ZoneId.of("UTC"), 210,
        )
        assertEquals(1, pendiente.rachaDias)
        assertEquals(false, pendiente.metaDiariaCumplida)
        assertEquals(1L, pendiente.segundosRestantesMetaDiaria)
        val cumplido = ProgressCalculator.calcular(
            listOf(ayer, hoy.copy(durationSeconds = 1_800)), hoy.finishedAt, ZoneId.of("UTC"), 210,
        )
        assertEquals(2, cumplido.rachaDias)
        assertEquals(true, cumplido.metaDiariaCumplida)
    }

    @Test
    fun entregaUnSoloBonusRetroactivoSinVolverAMultiplicarLosPuntosDelDia() {
        val primero = resultado("2026-10-07T12:00:00Z", duracionSegundos = 900).copy(pointsAwarded = 100)
        val segundo = resultado("2026-10-07T13:00:00Z", duracionSegundos = 900).copy(pointsAwarded = 100)
        val pendiente = aplicarMultiplicadorRacha(primero, emptyList(), 210)
        assertEquals(100, pendiente.pointsAwarded)
        assertEquals(0, pendiente.streakBonusPoints)
        val cumplido = aplicarMultiplicadorRacha(segundo, listOf(pendiente), 210)
        assertEquals(102, cumplido.pointsAwarded)
        assertEquals(2, cumplido.streakBonusPoints)
        assertEquals(200, cumplido.dailyBasePoints)
        val posterior = aplicarMultiplicadorRacha(
            resultado("2026-10-07T14:00:00Z").copy(pointsAwarded = 100),
            listOf(pendiente, cumplido),
            210,
        )
        assertEquals(100, posterior.pointsAwarded)
        assertEquals(0, posterior.streakBonusPoints)
    }

    @Test
    fun metaDiariaNoRedondeaHaciaAbajoLosMinutosYLaRachaSeCortaSiAyerNoCumplio() {
        val hoy = resultado("2026-10-07T12:00:00Z", duracionSegundos = 257)
        assertEquals(0, aplicarMultiplicadorRacha(hoy, emptyList(), 30).streakBonusPoints)
        assertEquals(1, aplicarMultiplicadorRacha(hoy.copy(durationSeconds = 258), emptyList(), 30).streakBonusPoints)
        val progreso = ProgressCalculator.calcular(
            listOf(
                resultado("2026-10-05T12:00:00Z"),
                resultado("2026-10-06T12:00:00Z", duracionSegundos = 1),
                hoy.copy(durationSeconds = 258),
            ),
            hoy.finishedAt, ZoneId.of("UTC"), 30,
        )
        assertEquals(1, progreso.rachaDias)
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