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

class RegistroViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val conectividad = FakeConnectivityMonitor()

    private fun crearViewModel() = RegistroViewModel(auth, conectividad)

    private fun RegistroViewModel.completar(
        username: String = "Ana",
        email: String = "ana@mail.com",
        password: String = "Secreto123",
    ) {
        onUsernameChange(username)
        onEmailChange(email)
        onPasswordChange(password)
    }

    @Test
    fun conDatosInvalidos_noLlamaAlRepositorioYExponeElErrorDeCadaCampo() = runTest {
        val vm = crearViewModel()
        vm.completar(username = "", email = "sin-arroba", password = "123")

        vm.registrar()

        assertTrue(auth.llamadasRegistrar.isEmpty())
        val errores = vm.uiState.value.errores
        assertEquals(ErrorCampo.VACIO, errores.username)
        assertEquals(ErrorCampo.FORMATO, errores.email)
        assertEquals(ErrorCampo.MUY_CORTO, errores.password)
    }

    @Test
    fun conDatosValidos_llamaARegistrarConElCorreoNormalizado() = runTest {
        val vm = crearViewModel()
        vm.completar(username = "  Ana  ", email = "  Ana@Mail.COM ", password = "Secreto123")

        vm.registrar()

        assertEquals(listOf(Triple("Ana", "ana@mail.com", "Secreto123")), auth.llamadasRegistrar)
        assertFalse(vm.uiState.value.enviando)
        assertNull(vm.uiState.value.errorEnvio)
    }

    @Test
    fun correoEnUso_produceElErrorDeCorreoEnUso() = runTest {
        auth.resultadoRegistrar = Resultado.Fallo(ErrorApp.CorreoEnUso)
        val vm = crearViewModel()
        vm.completar()

        vm.registrar()

        assertEquals(ErrorApp.CorreoEnUso, vm.uiState.value.errorEnvio)
        assertFalse(vm.uiState.value.enviando)
    }

    @Test
    fun sinConexion_noLlamaYExponeElAviso() = runTest {
        conectividad.establecer(Conectividad.SIN_CONEXION)
        val vm = crearViewModel()
        vm.completar()

        vm.registrar()

        assertTrue(auth.llamadasRegistrar.isEmpty())
        assertTrue(vm.uiState.value.sinConexion)
    }

    @Test
    fun alRecuperarLaConexion_seQuitaElAviso() = runTest {
        conectividad.establecer(Conectividad.SIN_CONEXION)
        val vm = crearViewModel()
        assertTrue(vm.uiState.value.sinConexion)

        conectividad.establecer(Conectividad.CONECTADO)

        assertFalse(vm.uiState.value.sinConexion)
    }

    @Test
    fun conUnaPeticionEnCurso_ignoraUnSegundoEnvio() = runTest {
        val compuerta = CompletableDeferred<Unit>()
        auth.compuerta = compuerta
        val vm = crearViewModel()
        vm.completar()

        vm.registrar()
        assertTrue(vm.uiState.value.enviando)
        vm.registrar() // doble toque

        assertEquals(1, auth.llamadasRegistrar.size)

        compuerta.complete(Unit)
        assertFalse(vm.uiState.value.enviando)
        assertEquals(1, auth.llamadasRegistrar.size)
    }

    @Test
    fun alEditarUnCampo_seQuitaSuError() = runTest {
        val vm = crearViewModel()
        vm.completar(email = "sin-arroba")
        vm.registrar()
        assertEquals(ErrorCampo.FORMATO, vm.uiState.value.errores.email)

        vm.onEmailChange("ana@mail.com")

        assertNull(vm.uiState.value.errores.email)
    }
}
