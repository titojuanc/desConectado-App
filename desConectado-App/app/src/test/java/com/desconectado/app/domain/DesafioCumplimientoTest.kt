package com.desconectado.app.domain

import com.desconectado.app.domain.model.Desafio
import com.desconectado.app.domain.model.Dificultad
import com.desconectado.app.domain.model.ChallengeResult
import com.desconectado.app.fakes.FakeTimeSource
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class DesafioCumplimientoTest {
    private val inicio = Instant.parse("2026-09-28T10:00:00Z")
    private val desafio = Desafio("facil", "Salir a caminar", "Sin redes", 30, Dificultad.FACIL, 10, 1)

    @Test fun usoCeroYDuracionCumplidaOtorgaPuntos() {
        val resultado = evaluarCumplimiento(desafio, 0, 0, inicio.plusSeconds(1800), FakeTimeSource(inicio.plusSeconds(1800)))
        assertEquals(ChallengeResult.Status.COMPLETED, resultado.status)
        assertEquals(10, resultado.pointsAwarded)
    }

    @Test fun usoPositivoNoOtorgaPuntos() {
        val resultado = evaluarCumplimiento(desafio, 1, 0, inicio.plusSeconds(1800), FakeTimeSource(inicio.plusSeconds(1800)))
        assertEquals(ChallengeResult.Status.FAILED, resultado.status)
        assertEquals(0, resultado.pointsAwarded)
    }

    @Test fun offlineDe300SegundosSeTolera() {
        val resultado = evaluarCumplimiento(desafio, 0, 300, inicio.plusSeconds(1800), FakeTimeSource(inicio.plusSeconds(1800)))
        assertEquals(ChallengeResult.Status.COMPLETED, resultado.status)
    }

    @Test fun offlineDe301SegundosInvalida() {
        val resultado = evaluarCumplimiento(desafio, 0, 301, inicio.plusSeconds(1800), FakeTimeSource(inicio.plusSeconds(1800)))
        assertEquals(ChallengeResult.Status.INVALIDATED, resultado.status)
        assertEquals(0, resultado.pointsAwarded)
    }
}
