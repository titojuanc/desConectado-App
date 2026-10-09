package com.desconectado.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class DeliveryResetTest {
    @Test
    fun unDesafioIniciadoAntesODuranteElResetSeDescartaYUnoPosteriorSeConserva() {
        val resetAt = Instant.parse("2026-10-08T12:00:00Z")

        assertTrue(desafioAnteriorAlReset(Instant.parse("2026-10-08T11:59:59Z"), resetAt))
        assertTrue(desafioAnteriorAlReset(resetAt, resetAt))
        assertFalse(desafioAnteriorAlReset(Instant.parse("2026-10-08T12:00:01Z"), resetAt))
    }
}