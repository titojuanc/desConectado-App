package com.desconectado.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class NombrePerfilTest {

    @Test
    fun nombreDeGoogleValido_seUsaRecortado() {
        assertEquals("Ana Pérez", nombreParaPerfil("  Ana Pérez ", "ana@mail.com"))
    }

    @Test
    fun nombreNulo_usaLaParteLocalDelCorreo() {
        assertEquals("ana.perez", nombreParaPerfil(null, "ana.perez@mail.com"))
    }

    @Test
    fun nombreEnBlanco_usaLaParteLocalDelCorreo() {
        assertEquals("ana", nombreParaPerfil("   ", "ana@mail.com"))
    }

    @Test
    fun nombreLargo_seRecortaATreintaCaracteres() {
        val resultado = nombreParaPerfil("a".repeat(40), "ana@mail.com")

        assertEquals(30, resultado.length)
    }

    @Test
    fun parteLocalLarga_seRecortaATreintaCaracteres() {
        val resultado = nombreParaPerfil(null, "${"b".repeat(45)}@mail.com")

        assertEquals("b".repeat(30), resultado)
    }

    @Test
    fun elCorteATreintaNoDejaEspaciosAlFinal() {
        // El carácter 30 es un espacio: el resultado no puede terminar en espacio.
        val resultado = nombreParaPerfil("a".repeat(29) + " bbb", "ana@mail.com")

        assertEquals("a".repeat(29), resultado)
    }
}
