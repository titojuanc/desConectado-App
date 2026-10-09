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
    val movementIds: List<String> = listOf(movementId),
    val kind: TipoRecompensa? = null,
    val config: Map<String, String> = emptyMap(),
    val grantedRewardId: String? = null,
    val grantedRewardName: String? = null,
    val grantedRewardKind: TipoRecompensa? = null,
    val grantedRewardConfig: Map<String, String> = emptyMap(),
)
