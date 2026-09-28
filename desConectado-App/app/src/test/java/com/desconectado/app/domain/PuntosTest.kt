package com.desconectado.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class PuntosTest {

    @Test
    fun formatearPuntos_muestraElNumeroCompletoConSeparadorDeMiles() {
        assertEquals("0", formatearPuntos(0))
        assertEquals("999", formatearPuntos(999))
        assertEquals("1.250", formatearPuntos(1250))
        assertEquals("1.234.567", formatearPuntos(1234567))
    }

    @Test
    fun normalizarSaldo_ausenteVale0() {
        assertEquals(0, normalizarSaldo(null))
    }

    @Test
    fun normalizarSaldo_respetaUnSaldoValido() {
        assertEquals(50, normalizarSaldo(50L))
        assertEquals(0, normalizarSaldo(0L))
    }

    @Test
    fun normalizarSaldo_nuncaMuestraUnSaldoNegativo() {
        assertEquals(0, normalizarSaldo(-3L))
    }

    @Test
    fun normalizarSaldo_unValorEnormeNoDesbordaElEntero() {
        assertEquals(Int.MAX_VALUE, normalizarSaldo(Long.MAX_VALUE))
    }

    @Test
    fun formatearFechaDesafio_muestraDiaMesYAnio() {
        val fecha = Instant.parse("2026-09-22T15:00:00Z")
        assertEquals("22/09/2026", formatearFechaDesafio(fecha, ZoneId.of("UTC")))
    }
}
