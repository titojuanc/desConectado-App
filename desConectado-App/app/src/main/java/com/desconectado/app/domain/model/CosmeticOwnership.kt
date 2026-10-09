package com.desconectado.app.domain.model

import java.time.Instant

data class CosmeticOwnership(
    val rewardId: String,
    val name: String,
    val kind: TipoRecompensa,
    val config: Map<String, String>,
    val acquiredAt: Instant,
    val redemptionId: String,
)