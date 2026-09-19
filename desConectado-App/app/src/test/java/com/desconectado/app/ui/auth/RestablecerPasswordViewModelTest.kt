package com.desconectado.app.ui.auth

import com.desconectado.app.domain.ErrorCampo
import com.desconectado.app.domain.model.Conectividad
import com.desconectado.app.domain.model.ErrorApp
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.fakes.FakeAuthRepository
import com.desconectado.app.fakes.FakeConnectivityMonitor
import com.desconectado.app.testutil.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RestablecerPasswordViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val conectividad = FakeConnectivityMonitor()

    private fun crearViewModel() = RestablecerPasswordViewModel(auth, conectividad)

    @Test
    fun unCorreoConFormatoInvalido_noLlamaYExponeElErrorJuntoAlCampo() = runTest {
        val vm = crearViewModel()
        vm.onEmailChange("sin-arroba")

        vm.enviar()

        assertTrue(auth.llamadasRestablecer.isEmpty())
        assertEquals(ErrorCampo.FORMATO, vm.uiState.value.errorCorreo)
        assertFalse(vm.uiState.value.confirmado)
    }

    @Test
    fun unCorreoVacio_noLlama() = runTest {
        val vm = crearViewModel()

        vm.enviar()

        assertTrue(auth.llamadasRestablecer.isEmpty())
        assertEquals(ErrorCampo.VACIO, vm.uiState.value.errorCorreo)
    }

    @Test
    fun unCorreoValido_llamaConElCorreoRecortadoYEnMinusculas() = runTest {
        val vm = crearViewModel()
        vm.onEmailChange("  Ana@Mail.COM ")

        vm.enviar()

        assertEquals(listOf("ana@mail.com"), auth.llamadasRestablecer)
    }

    @Test
    fun unExito_muestraElMensajeDeConfirmacion() = runTest {
        val vm = crearViewModel()
        vm.onEmailChange("ana@mail.com")

        vm.enviar()

        assertTrue(vm.uiState.value.confirmado)
        assertNull(vm.uiState.value.errorEnvio)
        assertFalse(vm.uiState.value.enviando)
    }

    @Test
    fun elMensajeEsElMismoParaUnaCuentaInexistente() = runTest {
        // El repositorio devuelve éxito exista o no la cuenta (FR-026): la pantalla no puede distinguirlo.
        val conCuenta = crearViewModel().apply { onEmailChange("ana@mail.com") }
        conCuenta.enviar()
        val sinCuenta = crearViewModel().apply { onEmailChange("nadie@mail.com") }
        sinCuenta.enviar()

        assertEquals(conCuenta.uiState.value.confirmado, sinCuenta.uiState.value.confirmado)
        assertTrue(sinCuenta.uiState.value.confirmado)
        assertEquals(conCuenta.uiState.value.errorEnvio, sinCuenta.uiState.value.errorEnvio)
    }

    @Test
    fun sinConexion_noLlamaYExponeElAviso() = runTest {
        conectividad.establecer(Conectividad.SIN_CONEXION)
        val vm = crearViewModel()
        vm.onEmailChange("ana@mail.com")

        vm.enviar()

        assertTrue(auth.llamadasRestablecer.isEmpty())
        assertTrue(vm.uiState.value.sinConexion)
        assertFalse(vm.uiState.value.confirmado)
    }

    @Test
    fun unSegundoEnvioMientrasElPrimeroEstaEnCurso_seIgnora() = runTest {
        val compuerta = CompletableDeferred<Unit>()
        auth.compuerta = compuerta
        val vm = crearViewModel()
        vm.onEmailChange("ana@mail.com")

        vm.enviar()
        assertTrue(vm.uiState.value.enviando)
        vm.enviar()

        assertEquals(1, auth.llamadasRestablecer.size)

        compuerta.complete(Unit)
        assertFalse(vm.uiState.value.enviando)
        assertTrue(vm.uiState.value.confirmado)
    }

    @Test
    fun sinConexionYDesconocidoDelRepositorio_muestranSuMensajeDeError() = runTest {
        auth.resultadoRestablecer = Resultado.Fallo(ErrorApp.SinConexion)
        val vmRed = crearViewModel().apply { onEmailChange("ana@mail.com") }
        vmRed.enviar()
        assertEquals(ErrorApp.SinConexion, vmRed.uiState.value.errorEnvio)
        assertFalse(vmRed.uiState.value.confirmado)

        auth.resultadoRestablecer = Resultado.Fallo(ErrorApp.Desconocido)
        val vmOtro = crearViewModel().apply { onEmailChange("ana@mail.com") }
        vmOtro.enviar()
        assertEquals(ErrorApp.Desconocido, vmOtro.uiState.value.errorEnvio)
        assertFalse(vmOtro.uiState.value.confirmado)
    }

    @Test
    fun alEditarElCorreo_seQuitanLaConfirmacionYLosErrores() = runTest {
        val vm = crearViewModel()
        vm.onEmailChange("ana@mail.com")
        vm.enviar()
        assertTrue(vm.uiState.value.confirmado)

        vm.onEmailChange("otra@mail.com")

        assertFalse(vm.uiState.value.confirmado)
        assertNull(vm.uiState.value.errorEnvio)
    }
}
