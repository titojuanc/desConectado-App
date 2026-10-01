package com.desconectado.app.domain

import com.desconectado.app.domain.model.Desafio
import com.desconectado.app.domain.model.Dificultad
import com.desconectado.app.domain.model.Recompensa
import com.desconectado.app.domain.model.TipoRecompensa
import com.desconectado.app.domain.model.Resultado
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CanjeTest {
    private val cupon = Recompensa("cupon", "Cupón", "Digital", 50, TipoRecompensa.CUPON, 1)
    private val insignia = Recompensa("insignia", "Insignia", "Digital", 50, TipoRecompensa.INSIGNIA, 1)

    @Test fun codigoEsDeterministaYNoContieneDatosPersonales() {
        val codigo = codigoCupon("redemption-123")
        assertEquals("DC-cmVkZW1wdGlvbi0xMjM", codigo)
        assertTrue(!codigo.contains("@"))
    }

    @Test fun saldoSuficientePermiteCanje() {
        assertTrue(validarCanje(50, cupon, false) is Resultado.Exito)
    }

    @Test fun saldoInsuficienteRechazaCanje() {
        assertTrue(validarCanje(49, cupon, false) is Resultado.Fallo)
    }

    @Test fun insigniaRepetidaSeRechaza() {
        assertTrue(validarCanje(100, insignia, true) is Resultado.Fallo)
    }

    @Test fun cuponRepetidoSePermite() {
        assertTrue(validarCanje(100, cupon, true) is Resultado.Exito)
    }
}
