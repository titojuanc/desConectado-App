package com.desconectado.app.domain

import com.desconectado.app.domain.model.Dificultad
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatoTest {

    @Test
    fun formatearDuracion_minutos() {
        assertEquals("30 minutos", formatearDuracion(30))
    }

    @Test
    fun formatearDuracion_unaHora() {
        assertEquals("1 hora", formatearDuracion(60))
    }

    @Test
    fun formatearDuracion_variasHoras() {
        assertEquals("2 horas", formatearDuracion(120))
        assertEquals("12 horas", formatearDuracion(720))
    }

    @Test
    fun formatearDuracion_horasYMinutos() {
        assertEquals("1 hora 30 minutos", formatearDuracion(90))
        assertEquals("2 horas 15 minutos", formatearDuracion(135))
    }

    @Test
    fun formatearDuracion_unSoloMinuto() {
        assertEquals("1 minuto", formatearDuracion(1))
    }

    @Test
    fun etiquetaDificultad_usaFacilNormalYDificil() {
        assertEquals("Fácil", etiquetaDificultad(Dificultad.FACIL))
        assertEquals("Normal", etiquetaDificultad(Dificultad.NORMAL))
        assertEquals("Difícil", etiquetaDificultad(Dificultad.DIFICIL))
    }
}
