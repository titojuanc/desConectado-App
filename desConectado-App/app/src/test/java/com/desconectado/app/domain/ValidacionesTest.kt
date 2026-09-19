package com.desconectado.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidacionesTest {

    // --- normalizarCorreo ---

    @Test
    fun normalizarCorreo_recortaEspaciosYPasaAMinusculas() {
        assertEquals("ana@mail.com", normalizarCorreo("  Ana@Mail.COM "))
    }

    // --- validarCorreo ---

    @Test
    fun validarCorreo_aceptaUnCorreoValido() {
        assertNull(validarCorreo("a@b.co"))
    }

    @Test
    fun validarCorreo_normalizaAntesDeValidar() {
        assertNull(validarCorreo("  Ana@Mail.COM "))
    }

    @Test
    fun validarCorreo_rechazaFormatosInvalidos() {
        assertEquals(ErrorCampo.FORMATO, validarCorreo("sin-arroba"))
        assertEquals(ErrorCampo.FORMATO, validarCorreo("a@"))
        assertEquals(ErrorCampo.FORMATO, validarCorreo("@b.co"))
    }

    @Test
    fun validarCorreo_rechazaUnCorreoVacio() {
        assertEquals(ErrorCampo.VACIO, validarCorreo("   "))
    }

    // --- validarPassword ---

    @Test
    fun validarPassword_rechazaSieteCaracteres() {
        assertEquals(ErrorCampo.MUY_CORTO, validarPassword("1234567"))
    }

    @Test
    fun validarPassword_aceptaOchoCaracteres() {
        assertNull(validarPassword("12345678"))
    }

    @Test
    fun validarPassword_aceptaEspaciosYCaracteresEspecialesTalCual() {
        assertNull(validarPassword("        "))
        assertNull(validarPassword("p@ss w0rd!ñ"))
    }

    @Test
    fun validarPassword_noRecortaLosEspacios() {
        // Siete caracteres más un espacio al final: son ocho, y el espacio cuenta.
        assertNull(validarPassword("1234567 "))
    }

    @Test
    fun validarPassword_rechazaUnaContrasenaVacia() {
        assertEquals(ErrorCampo.VACIO, validarPassword(""))
    }

    // --- validarUsername ---

    @Test
    fun validarUsername_rechazaDosCaracteres() {
        assertEquals(ErrorCampo.MUY_CORTO, validarUsername("ab"))
    }

    @Test
    fun validarUsername_aceptaTresYTreintaCaracteres() {
        assertNull(validarUsername("abc"))
        assertNull(validarUsername("a".repeat(30)))
    }

    @Test
    fun validarUsername_rechazaTreintaYUnCaracteres() {
        assertEquals(ErrorCampo.MUY_LARGO, validarUsername("a".repeat(31)))
    }

    @Test
    fun validarUsername_recortaEspaciosAntesDeMedir() {
        assertEquals(ErrorCampo.MUY_CORTO, validarUsername("  ab  "))
        assertNull(validarUsername("  abc  "))
        assertNull(validarUsername(" " + "a".repeat(30) + " "))
    }

    @Test
    fun validarUsername_rechazaUnNombreEnBlanco() {
        assertEquals(ErrorCampo.VACIO, validarUsername("     "))
    }

    // --- validarRegistro ---

    @Test
    fun validarRegistro_devuelveTodosLosCamposInvalidosALaVezConSuMotivo() {
        val errores = validarRegistro(username = "", email = "sin-arroba", password = "123")

        assertFalse(errores.esValido)
        assertEquals(ErrorCampo.VACIO, errores.username)
        assertEquals(ErrorCampo.FORMATO, errores.email)
        assertEquals(ErrorCampo.MUY_CORTO, errores.password)
    }

    @Test
    fun validarRegistro_marcaSoloLosCamposInvalidos() {
        val errores = validarRegistro(username = "Ana", email = "ana@mail.com", password = "corta")

        assertFalse(errores.esValido)
        assertNull(errores.username)
        assertNull(errores.email)
        assertEquals(ErrorCampo.MUY_CORTO, errores.password)
    }

    @Test
    fun validarRegistro_aceptaDatosValidos() {
        val errores = validarRegistro(username = "Ana", email = "ana@mail.com", password = "Secreto123")

        assertTrue(errores.esValido)
    }
}
