package com.desconectado.app.domain

import com.desconectado.app.domain.model.PointLot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class UpcomingPointExpiryTest {
    private val zone = ZoneId.of("America/Los_Angeles")
    private val now = Instant.parse("2026-10-06T12:00:00Z")

    @Test
    fun sumaLotesDelSiguienteVencimientoYCuentaDiasEnLaZonaDelLote() {
        val first = PointLot.crear(20, Instant.parse("2026-10-05T16:00:00Z"), zone)
        val sameExpiry = first.copy(lotId = "another-credit", issuedPoints = 15, remainingPoints = 15)
        val later = PointLot.crear(50, Instant.parse("2026-10-06T16:00:00Z"), zone)

        val upcoming = proximoVencimiento(listOf(later, sameExpiry, first), now)

        assertEquals(35, upcoming?.points)
        assertEquals(first.expiresAt, upcoming?.expiresAt)
        assertEquals(29L, upcoming?.daysRemaining)
    }

    @Test
    fun ignoraLotesVencidosOAgotados() {
        val expired = PointLot.crear(10, Instant.parse("2026-08-01T12:00:00Z"), zone)
        val spent = PointLot.crear(10, Instant.parse("2026-10-05T16:00:00Z"), zone)
            .copy(remainingPoints = 0)

        assertNull(proximoVencimiento(listOf(expired, spent), now))
    }
}