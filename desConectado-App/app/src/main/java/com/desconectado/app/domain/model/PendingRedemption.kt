package com.desconectado.app.domain.model

import java.time.Instant

data class PendingRedemption(
    val redemptionId: String,
    val rewardId: String,
    val name: String,
    val costPoints: Int,
    val code: String?,
    val pointsDebited: Int,
    val lotDebits: Map<String, Int>,
    val createdAt: Instant,
    val kind: TipoRecompensa? = null,
    val config: Map<String, String> = emptyMap(),
    val grantedRewardId: String? = null,
    val grantedRewardName: String? = null,
    val grantedRewardKind: TipoRecompensa? = null,
    val grantedRewardConfig: Map<String, String> = emptyMap(),
) {
    val pointsRemaining: Int
        get() = costPoints - pointsDebited

    val canFinalize: Boolean
        get() = pointsDebited == costPoints

    init {
        require(costPoints > 0)
        require(pointsDebited in 0..costPoints)
        require(lotDebits.values.all { it > 0 })
        require(lotDebits.values.sum() == pointsDebited)
    }

    fun registrarDebito(lotId: String, points: Int): PendingRedemption {
        require(points > 0)
        lotDebits[lotId]?.let {
            require(it == points)
            return this
        }
        require(points <= pointsRemaining)
        return copy(
            pointsDebited = pointsDebited + points,
            lotDebits = lotDebits + (lotId to points),
        )
    }
}