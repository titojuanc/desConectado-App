package com.desconectado.app.domain

import com.desconectado.app.domain.model.PendingRedemption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class PendingRedemptionTest {
    private val pending = PendingRedemption(
        redemptionId = "redemption-1",
        rewardId = "theme-1",
        name = "Tema",
        costPoints = 70,
        code = null,
        pointsDebited = 0,
        lotDebits = emptyMap(),
        createdAt = Instant.parse("2026-10-05T10:00:00Z"),
    )

    @Test
    fun registraDebitosDeLotesHastaCompletarElCosto() {
        val partial = pending.registrarDebito("lot-1", 40)
        val complete = partial.registrarDebito("lot-2", 30)

        assertEquals(40, partial.pointsDebited)
        assertEquals(30, partial.pointsRemaining)
        assertFalse(partial.canFinalize)
        assertEquals(70, complete.pointsDebited)
        assertEquals(0, complete.pointsRemaining)
        assertTrue(complete.canFinalize)
    }

    @Test
    fun reintentarElMismoDebitoNoLoDuplica() {
        val once = pending.registrarDebito("lot-1", 40)
        val retried = once.registrarDebito("lot-1", 40)

        assertSame(once, retried)
    }

    @Test
    fun elMismoLoteNoPuedeReintentarseConUnImporteDistinto() {
        val once = pending.registrarDebito("lot-1", 40)

        assertTrue(runCatching { once.registrarDebito("lot-1", 30) }.isFailure)
    }

    @Test
    fun noPermiteDebitarMasQueElCostoTotal() {
        val partial = pending.registrarDebito("lot-1", 50)

        assertTrue(runCatching { partial.registrarDebito("lot-2", 21) }.isFailure)
    }
}