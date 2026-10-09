package com.desconectado.app.domain

import com.desconectado.app.domain.model.Recompensa
import com.desconectado.app.domain.model.TipoRecompensa
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CosmeticBoxSelectionTest {
    private val tema = recompensa("theme-bosque", TipoRecompensa.TEMA)
    private val fondo = recompensa("background-montanas", TipoRecompensa.FONDO_ENFOQUE)
    private val cupon = recompensa("coupon-demo", TipoRecompensa.CUPON)
    private val insignia = recompensa("badge-legacy", TipoRecompensa.INSIGNIA)
    private val caja = recompensa("surprise-box", TipoRecompensa.CAJA_SORPRESA)

    @Test
    fun seleccionaSoloCosmeticosActivosQueNoPoseeLaPersona() {
        val catalogo = listOf(tema, fondo, cupon, insignia, caja)

        val seleccionado = seleccionarPremioSorpresa(
            recompensas = catalogo,
            ownedRewardIds = setOf(tema.id),
            redemptionId = "redemption-1",
        )

        assertEquals(fondo.id, seleccionado?.id)
    }

    @Test
    fun laSeleccionEsDeterministaParaElMismoCanjeYExcluyeElPremioYaPoseido() {
        val catalogo = listOf(tema, fondo)
        val primero = seleccionarPremioSorpresa(catalogo, emptySet(), "redemption-42")
        val reintento = seleccionarPremioSorpresa(catalogo, emptySet(), "redemption-42")
        val siguienteCaja = seleccionarPremioSorpresa(catalogo, setOf(primero!!.id), "redemption-43")

        assertEquals(primero, reintento)
        assertNotEquals(primero.id, siguienteCaja?.id)
    }

    @Test
    fun noHayPremioYNoSeDebeCobrarSiNoQuedanCosmeticosElegibles() {
        val seleccionado = seleccionarPremioSorpresa(
            recompensas = listOf(tema.copy(active = false), cupon, insignia, caja),
            ownedRewardIds = emptySet(),
            redemptionId = "redemption-1",
        )

        assertNull(seleccionado)
    }

    @Test
    fun losArticulosElegiblesPertenecenSoloATiposCosmeticos() {
        val elegibles = listOf(tema, fondo).filter { it.kind.esCosmetico() }

        assertTrue(elegibles.isNotEmpty())
        assertTrue(listOf(cupon, insignia, caja).none { it.kind.esCosmetico() })
    }

    private fun recompensa(id: String, tipo: TipoRecompensa) = Recompensa(
        id = id,
        name = id,
        description = "Cosmético de la app.",
        costPoints = 100,
        kind = tipo,
        order = 1,
    )
}