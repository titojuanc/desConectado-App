package com.desconectado.app.ui.auth

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

class IngresoViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val conectividad = FakeConnectivityMonitor()

    private fun crearViewModel() = IngresoViewModel(auth, conectividad)

    private fun IngresoViewModel.completar(
        email: String = "ana@mail.com",
        password: String = "Secreto123",
    ) {
        onEmailChange(email)
        onPasswordChange(password)
    }

    @Test
    fun credencialesInvalidas_produceUnUnicoErrorGenerico() = runTest {
        auth.resultadoIngresar = Resultado.Fallo(ErrorApp.CredencialesInvalidas)
        val vm = crearViewModel()
        vm.completar()

        vm.ingresar()

        // Un solo error de envío, sin errores por campo: no revela si falló el correo o la contraseña.
        val estado = vm.uiState.value
        assertEquals(ErrorApp.CredencialesInvalidas, estado.errorEnvio)
        assertNull(estado.errorCorreo)
        assertNull(estado.errorPassword)
    }

    @Test
    fun elMismoErrorAparece_seaCualSeaElCampoIncorrecto() = runTest {
        auth.resultadoIngresar = Resultado.Fallo(ErrorApp.CredencialesInvalidas)

        val correoInexistente = crearViewModel().apply { completar(email = "nadie@mail.com") }
        correoInexistente.ingresar()
        val passwordIncorrecta = crearViewModel().apply { completar(password = "incorrecta1") }
        passwordIncorrecta.ingresar()

        assertEquals(correoInexistente.uiState.value.errorEnvio, passwordIncorrecta.uiState.value.errorEnvio)
    }

    @Test
    fun sinConexion_noLlama() = runTest {
        conectividad.establecer(Conectividad.SIN_CONEXION)
        val vm = crearViewModel()
        vm.completar()

        vm.ingresar()

        assertTrue(auth.llamadasIngresar.isEmpty())
        assertTrue(vm.uiState.value.sinConexion)
    }

    @Test
    fun dobleToque_seIgnora() = runTest {
        val compuerta = CompletableDeferred<Unit>()
        auth.compuerta = compuerta
        val vm = crearViewModel()
        vm.completar()

        vm.ingresar()
        vm.ingresar()

        assertEquals(1, auth.llamadasIngresar.size)
        assertTrue(vm.uiState.value.enviando)

        compuerta.complete(Unit)
        assertFalse(vm.uiState.value.enviando)
    }

    @Test
    fun elCorreoSeNormalizaAntesDeLlamar() = runTest {
        val vm = crearViewModel()
        vm.completar(email = "  Ana@Mail.COM ", password = "Secreto123")

        vm.ingresar()

        assertEquals(listOf("ana@mail.com" to "Secreto123"), auth.llamadasIngresar)
    }

    @Test
    fun conCamposVacios_noLlamaYMarcaCadaCampo() = runTest {
        val vm = crearViewModel()

        vm.ingresar()

        assertTrue(auth.llamadasIngresar.isEmpty())
        assertEquals(com.desconectado.app.domain.ErrorCampo.VACIO, vm.uiState.value.errorCorreo)
        assertEquals(com.desconectado.app.domain.ErrorCampo.VACIO, vm.uiState.value.errorPassword)
    }
}
