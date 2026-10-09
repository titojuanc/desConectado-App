package com.desconectado.app.domain

import com.desconectado.app.domain.model.ChallengeResult
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.fakes.FakeAchievementRepository
import com.desconectado.app.fakes.FakeChallengeRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class AchievementRefreshTest {
    @Test
    fun unResultadoCompletadoActualizaLogrosUsandoTodoElHistorial() = kotlinx.coroutines.test.runTest {
        val resultado = resultado(ChallengeResult.Status.COMPLETED)
        val historial = listOf(resultado.copy(challengeRunId = "run-anterior"), resultado)
        val desafios = FakeChallengeRepository().apply { resultsResult = Resultado.Exito(historial) }
        val logros = FakeAchievementRepository()

        actualizarLogrosTrasResultado("uid-1", resultado, desafios, logros)

        assertEquals(listOf("uid-1"), desafios.resultsCalls)
        assertEquals(listOf("uid-1" to historial), logros.llamadas)
    }

    @Test
    fun resultadoNoCompletadoNoActualizaLogros() = kotlinx.coroutines.test.runTest {
        val desafios = FakeChallengeRepository()
        val logros = FakeAchievementRepository()

        actualizarLogrosTrasResultado(
            "uid-1",
            resultado(ChallengeResult.Status.FAILED),
            desafios,
            logros,
        )

        assertEquals(emptyList<String>(), desafios.resultsCalls)
        assertEquals(emptyList<Any>(), logros.llamadas)
    }

    private fun resultado(status: ChallengeResult.Status) = ChallengeResult(
        challengeRunId = "run-1",
        challengeId = "move-1",
        challengeTitle = "Salir a caminar",
        durationMinutes = 30,
        startedAt = Instant.parse("2026-10-07T11:30:00Z"),
        finishedAt = Instant.parse("2026-10-07T12:00:00Z"),
        status = status,
        measuredSocialSeconds = 0,
        offlineSeconds = 0,
        pointsAwarded = if (status == ChallengeResult.Status.COMPLETED) 10 else 0,
    )
}