package com.desconectado.app.domain

import com.desconectado.app.domain.model.CosmeticOwnership
import com.desconectado.app.domain.model.RedeemedReward
import com.desconectado.app.domain.model.TipoRecompensa
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class CosmeticOwnershipTest {
    @Test
    fun reconstruyeCosmeticosCompradosYPremiosDeCajaSinIncluirCuponesNiInsignias() {
        val canjes = listOf(
            canje("r-theme", "theme-bosque", TipoRecompensa.TEMA, mapOf("palette" to "forest")),
            canje(
                id = "r-box",
                rewardId = "surprise-box",
                kind = TipoRecompensa.CAJA_SORPRESA,
                grantId = "background-montanas",
                grantName = "Fondo Montañas",
                grantKind = TipoRecompensa.FONDO_ENFOQUE,
                grantConfig = mapOf("background" to "mountains"),
            ),
            canje("r-coupon", "coupon-demo", TipoRecompensa.CUPON),
            canje("r-badge", "badge-legacy", TipoRecompensa.INSIGNIA),
        )

        val propiedad = cosmeticosPoseidos(canjes)

        assertEquals(listOf("theme-bosque", "background-montanas"), propiedad.map { it.rewardId })
        assertEquals(listOf(TipoRecompensa.TEMA, TipoRecompensa.FONDO_ENFOQUE), propiedad.map { it.kind })
        assertEquals(mapOf("background" to "mountains"), propiedad.last().config)
    }

    private fun canje(
        id: String,
        rewardId: String,
        kind: TipoRecompensa,
        config: Map<String, String> = emptyMap(),
        grantId: String? = null,
        grantName: String? = null,
        grantKind: TipoRecompensa? = null,
        grantConfig: Map<String, String> = emptyMap(),
    ) = RedeemedReward(
        redemptionId = id,
        rewardId = rewardId,
        name = rewardId,
        costPoints = 100,
        movementId = "movement-$id",
        code = null,
        createdAt = Instant.parse("2026-10-08T10:00:00Z"),
        kind = kind,
        config = config,
        grantedRewardId = grantId,
        grantedRewardName = grantName,
        grantedRewardKind = grantKind,
        grantedRewardConfig = grantConfig,
    )
}