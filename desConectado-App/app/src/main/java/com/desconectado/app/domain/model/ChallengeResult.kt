package com.desconectado.app.domain.model

import java.time.Instant
import java.time.ZoneId

data class ChallengeResult(
    val challengeRunId: String,
    val challengeId: String,
    val challengeTitle: String,
    val durationMinutes: Int,
    val startedAt: Instant,
    val finishedAt: Instant,
    val status: Status,
    val measuredSocialSeconds: Long,
    val offlineSeconds: Long,
    val pointsAwarded: Int,
    val durationSeconds: Int = durationMinutes * 60,
    val timeZoneId: String = ZoneId.systemDefault().id,
) {
    enum class Status { COMPLETED, FAILED, CANCELLED, INVALIDATED }
}
