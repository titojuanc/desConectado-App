package com.desconectado.app.domain

import com.desconectado.app.domain.model.PointLot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class PointLotTest {
    private val zonaInicial = ZoneId.of("America/Los_Angeles")
    private val ganado = Instant.parse("2026-03-08T08:30:00Z")

    @Test
    fun creaLoteHastaLaMedianocheLocalYExpiraAlInicioDelDiaTreinta() {
        val lote = PointLot.crear(40, ganado, zonaInicial)

        assertEquals(LocalDate.parse("2026-03-08"), lote.localDate)
        assertEquals("America/Los_Angeles", lote.timeZoneId)
        assertEquals(Instant.parse("2026-03-09T07:00:00Z"), lote.windowEndsAt)
        assertEquals(Instant.parse("2026-04-07T07:00:00Z"), lote.expiresAt)
    }

    @Test
    fun agregaCreditosPosterioresAlLoteAunqueCambieLaZonaDelDispositivo() {
        val lote = PointLot.crear(40, ganado, zonaInicial)
        val creditoDespuesDelCambio = Instant.parse("2026-03-09T05:00:00Z")

        val actualizado = lote.agregar(15, creditoDespuesDelCambio)

        assertEquals(55, actualizado.issuedPoints)
        assertEquals(55, actualizado.remainingPoints)
        assertEquals(zonaInicial.id, actualizado.timeZoneId)
        assertEquals(lote.expiresAt, actualizado.expiresAt)
    }

    @Test
    fun noAgregaCreditoDespuesDelCierreDeLaSesionLocal() {
        val lote = PointLot.crear(40, ganado, zonaInicial)

        assertThrows(IllegalArgumentException::class.java) {
            lote.agregar(10, lote.windowEndsAt)
        }
    }

    @Test
    fun consumoFifoReduceSoloElRemanenteDelLoteMasAntiguo() {
        val primerLote = PointLot.crear(30, ganado, zonaInicial)
        val segundoLote = PointLot.crear(40, Instant.parse("2026-03-10T12:00:00Z"), zonaInicial)

        val lotes = consumirPuntosFifo(
            listOf(segundoLote, primerLote),
            points = 20,
            moment = Instant.parse("2026-03-11T12:00:00Z"),
        )

        assertEquals(10, lotes.single { it.localDate == primerLote.localDate }.remainingPoints)
        assertEquals(40, lotes.single { it.localDate == segundoLote.localDate }.remainingPoints)
    }

    @Test
    fun soloVenceElRemanenteDesdeLaMedianocheDelDiaTreinta() {
        val lote = PointLot.crear(40, ganado, zonaInicial).copy(remainingPoints = 10)

        assertEquals(0, lote.puntosVencidos(lote.expiresAt.minusNanos(1)))
        assertEquals(10, lote.puntosVencidos(lote.expiresAt))
        assertEquals(10, lote.puntosVencidos(lote.expiresAt.plusSeconds(1)))
    }

    @Test
    fun rechazaConsumoQueSuperaLosPuntosDisponibles() {
        val lote = PointLot.crear(30, ganado, zonaInicial)

        assertThrows(IllegalArgumentException::class.java) {
            consumirPuntosFifo(listOf(lote), 31, Instant.parse("2026-03-09T12:00:00Z"))
        }
    }
}