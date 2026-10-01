package com.desconectado.app.domain.model

import java.time.Instant

data class RedeemedReward(
    val redemptionId: String,
    val rewardId: String,
    val name: String,
    val costPoints: Int,
    val movementId: String,
    val code: String?,
    val createdAt: Instant,
)
