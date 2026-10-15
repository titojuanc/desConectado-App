package com.desconectado.app.domain

import com.desconectado.app.domain.model.ChallengeResult
import java.time.ZoneId

data class DailyStreakProgress(
    val basePoints: Int = 0,
    val completedSeconds: Long = 0,
    val bonusAwarded: Boolean = false,
)

fun aplicarMultiplicadorRacha(
    resultado: ChallengeResult,
    historialPrevio: List<ChallengeResult>,
    metaSemanalMinutos: Int?,
    progresoDiario: DailyStreakProgress? = null,
): ChallengeResult {
    if (resultado.status != ChallengeResult.Status.COMPLETED || metaSemanalMinutos == null) return resultado
    require(metaSemanalMinutos in ProgressCalculator.META_MINIMA_MINUTOS..ProgressCalculator.META_MAXIMA_MINUTOS)
    require(metaSemanalMinutos % 30 == 0)
    val zona = ZoneId.of(resultado.timeZoneId)
    val hoy = resultado.finishedAt.atZone(zona).toLocalDate()
    val anteriores = historialPrevio.distinctBy { it.challengeRunId }.filter {
        it.challengeRunId != resultado.challengeRunId && it.status == ChallengeResult.Status.COMPLETED &&
            !it.finishedAt.isAfter(resultado.finishedAt)
    }
    val deHoy = anteriores.filter {
        it.finishedAt.atZone(ZoneId.of(it.timeZoneId)).toLocalDate() == hoy
    }
    val diario = progresoDiario ?: DailyStreakProgress(
        basePoints = deHoy.sumOf { it.pointsAwarded - it.streakBonusPoints },
        completedSeconds = deHoy.sumOf { it.durationSeconds.toLong() },
        bonusAwarded = deHoy.any { it.streakBonusPoints > 0 },
    )
    if (diario.bonusAwarded || (diario.completedSeconds + resultado.durationSeconds) * 7 < metaSemanalMinutos * 60L) {
        return resultado
    }
    val rachaDias = ProgressCalculator.calcular(
        resultados = anteriores + resultado,
        ahora = resultado.finishedAt,
        zonaActual = zona,
        metaSemanalMinutos = metaSemanalMinutos,
    ).rachaDias
    val puntosBase = Math.addExact(diario.basePoints, resultado.pointsAwarded)
    val numerador = puntosBase.toLong() * rachaDias * metaSemanalMinutos
    val bono = Math.toIntExact((numerador + 20_999L) / 21_000L)
    return resultado.copy(
        pointsAwarded = Math.addExact(resultado.pointsAwarded, bono),
        pointsMultiplier = 1.0 + rachaDias * metaSemanalMinutos / 21_000.0,
        streakDays = rachaDias,
        weeklyGoalMinutesUsed = metaSemanalMinutos,
        streakBonusPoints = bono,
        dailyBasePoints = puntosBase,
    )
}
