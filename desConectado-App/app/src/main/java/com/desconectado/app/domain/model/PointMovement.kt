package com.desconectado.app.domain.model

import java.time.Instant

data class PointMovement(
    val id: String,
    val type: Type,
    val amount: Int,
    val challengeId: String? = null,
    val rewardId: String? = null,
    val sourceId: String,
    val createdAt: Instant,
    val code: String? = null,
) {
    enum class Type { CREDIT, REDEEM, EXPIRE }
}
