package com.desconectado.app.domain

import com.desconectado.app.domain.model.ChallengeResult
import com.desconectado.app.domain.model.Desafio
import java.time.Instant

fun evaluarCumplimiento(
    desafio: Desafio,
    measuredSocialSeconds: Long,
    offlineSeconds: Long,
    finishedAt: Instant,
    timeSource: TimeSource,
    startedAt: Instant = finishedAt.minusSeconds((desafio.durationSeconds ?: desafio.durationMinutes * 60).toLong()),
): ChallengeResult {
    val durationSeconds = desafio.durationSeconds ?: desafio.durationMinutes * 60
    val status = when {
        offlineSeconds > MAX_OFFLINE_SECONDS -> ChallengeResult.Status.INVALIDATED
        timeSource.elapsedSeconds(startedAt, finishedAt) < durationSeconds -> ChallengeResult.Status.FAILED
        measuredSocialSeconds > 0 -> ChallengeResult.Status.FAILED
        else -> ChallengeResult.Status.COMPLETED
    }
    return ChallengeResult(
        challengeRunId = "${desafio.id}-${startedAt.epochSecond}",
        challengeId = desafio.id,
        challengeTitle = desafio.title,
        durationMinutes = desafio.durationMinutes,
        startedAt = startedAt,
        finishedAt = finishedAt,
        status = status,
        measuredSocialSeconds = measuredSocialSeconds.coerceAtLeast(0),
        offlineSeconds = offlineSeconds.coerceAtLeast(0),
        pointsAwarded = if (status == ChallengeResult.Status.COMPLETED) desafio.points else 0,
        durationSeconds = durationSeconds,
        category = desafio.category,
    )
}

private const val MAX_OFFLINE_SECONDS = 300L
