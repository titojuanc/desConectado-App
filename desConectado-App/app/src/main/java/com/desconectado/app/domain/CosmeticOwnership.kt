package com.desconectado.app.domain

import com.desconectado.app.domain.model.CosmeticOwnership
import com.desconectado.app.domain.model.RedeemedReward

fun cosmeticosPoseidos(canjes: List<RedeemedReward>): List<CosmeticOwnership> {
    val propiedad = linkedMapOf<String, CosmeticOwnership>()
    canjes.forEach { canje ->
        if (canje.kind?.esCosmetico() == true) {
            propiedad.putIfAbsent(canje.rewardId, CosmeticOwnership(
                rewardId = canje.rewardId,
                name = canje.name,
                kind = canje.kind,
                config = canje.config,
                acquiredAt = canje.createdAt,
                redemptionId = canje.redemptionId,
            ))
        }
        val kind = canje.grantedRewardKind
        val rewardId = canje.grantedRewardId
        val name = canje.grantedRewardName
        if (kind?.esCosmetico() == true && rewardId != null && name != null) {
            propiedad.putIfAbsent(rewardId, CosmeticOwnership(
                rewardId = rewardId,
                name = name,
                kind = kind,
                config = canje.grantedRewardConfig,
                acquiredAt = canje.createdAt,
                redemptionId = canje.redemptionId,
            ))
        }
    }
    return propiedad.values.toList()
}