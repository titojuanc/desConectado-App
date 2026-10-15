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
    val category: CategoriaDesafio? = null,
    val pointsMultiplier: Double = 1.0,
    val streakDays: Int = 0,
    val weeklyGoalMinutesUsed: Int = 0,
    val streakBonusPoints: Int = 0,
    val dailyBasePoints: Int = 0,
    val dailyKey: String = "",
    val invalidationReason: String? = null,
) {
    enum class Status { COMPLETED, FAILED, CANCELLED, INVALIDATED }
}
