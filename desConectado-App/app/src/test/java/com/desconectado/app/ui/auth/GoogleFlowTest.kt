package com.desconectado.app.ui.auth

import com.desconectado.app.domain.model.Conectividad
import com.desconectado.app.domain.model.ErrorApp
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.fakes.FakeAuthRepository
import com.desconectado.app.fakes.FakeConnectivityMonitor
import com.desconectado.app.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Flujo "Continuar con Google" en Ingreso y en Registro (Historia 4). */
class GoogleFlowTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val conectividad = FakeConnectivityMonitor()

    private fun ingreso() = IngresoViewModel(auth, conectividad)
    private fun registro() = RegistroViewModel(auth, conectividad)

    // --- Ingreso ---

    @Test
    fun ingreso_cancelar_noMuestraErrorNiCambiaElEstado() = runTest {
        val vm = ingreso()
        val antes = vm.uiState.value

        vm.continuarConGoogle(Resultado.Fallo(ErrorApp.Cancelado))

        assertEquals(antes, vm.uiState.value)
        assertTrue(auth.llamadasIngresarConGoogle.isEmpty())
    }

    @Test
    fun ingreso_conTokenObtenido_llamaAIngresarConGoogle() = runTest {
        val vm = ingreso()

        vm.continuarConGoogle(Resultado.Exito("token-1"))

        assertEquals(listOf("token-1"), auth.llamadasIngresarConGoogle)
        assertFalse(vm.uiState.value.enviando)
        assertNull(vm.uiState.value.errorEnvio)
    }

    @Test
    fun ingreso_sinConexion_bloqueaYNoLlama() = runTest {
        conectividad.establecer(Conectividad.SIN_CONEXION)
        val vm = ingreso()

        vm.continuarConGoogle(Resultado.Exito("token-1"))

        assertTrue(auth.llamadasIngresarConGoogle.isEmpty())
        assertEquals(ErrorApp.SinConexion, vm.uiState.value.errorEnvio)
    }

    @Test
    fun ingreso_siNoSeObtieneElToken_muestraSuError() = runTest {
        val vm = ingreso()

        vm.continuarConGoogle(Resultado.Fallo(ErrorApp.Desconocido))

        assertEquals(ErrorApp.Desconocido, vm.uiState.value.errorEnvio)
        assertTrue(auth.llamadasIngresarConGoogle.isEmpty())
    }

    @Test
    fun ingreso_conCuentaExistenteConOtroProveedor_abreLaVinculacionYLaCompletaConLaContrasena() = runTest {
        auth.resultadoIngresarConGoogle = Resultado.Fallo(ErrorApp.CuentaExistenteConOtroProveedor)
        val vm = ingreso()
        vm.onEmailChange("  Ana@Mail.COM ")

        vm.continuarConGoogle(Resultado.Exito("token-1"))

        val pendiente = vm.uiState.value.vinculacion
        assertNotNull(pendiente)
        assertEquals("token-1", pendiente!!.idToken)
        assertEquals("ana@mail.com", pendiente.email)
        assertNull(vm.uiState.value.errorEnvio)

        vm.onVinculacionPasswordChange("Secreto123")
        vm.confirmarVinculacion()

        assertEquals(listOf("ana@mail.com" to "Secreto123"), auth.llamadasIngresar)
        assertEquals(listOf("token-1"), auth.llamadasVincularGoogle)
        assertNull(vm.uiState.value.vinculacion)
    }

    @Test
    fun ingreso_vinculacionConContrasenaIncorrecta_muestraElErrorYNoVincula() = runTest {
        auth.resultadoIngresarConGoogle = Resultado.Fallo(ErrorApp.CuentaExistenteConOtroProveedor)
        auth.resultadoIngresar = Resultado.Fallo(ErrorApp.CredencialesInvalidas)
        val vm = ingreso()
        vm.onEmailChange("ana@mail.com")
        vm.continuarConGoogle(Resultado.Exito("token-1"))
        vm.onVinculacionPasswordChange("mal")

        vm.confirmarVinculacion()

        assertTrue(auth.llamadasVincularGoogle.isEmpty())
        assertEquals(ErrorApp.CredencialesInvalidas, vm.uiState.value.vinculacion?.error)
        assertFalse(vm.uiState.value.vinculacion!!.enviando)
    }

    @Test
    fun ingreso_cancelarLaVinculacion_cierraElDialogoSinLlamar() = runTest {
        auth.resultadoIngresarConGoogle = Resultado.Fallo(ErrorApp.CuentaExistenteConOtroProveedor)
        val vm = ingreso()
        vm.continuarConGoogle(Resultado.Exito("token-1"))
        assertNotNull(vm.uiState.value.vinculacion)

        vm.cancelarVinculacion()

        assertNull(vm.uiState.value.vinculacion)
        assertTrue(auth.llamadasIngresar.isEmpty())
        assertTrue(auth.llamadasVincularGoogle.isEmpty())
    }

    // --- Registro ---

    @Test
    fun registro_cancelar_noMuestraErrorNiCambiaElEstado() = runTest {
        val vm = registro()
        val antes = vm.uiState.value

        vm.continuarConGoogle(Resultado.Fallo(ErrorApp.Cancelado))

        assertEquals(antes, vm.uiState.value)
        assertTrue(auth.llamadasIngresarConGoogle.isEmpty())
    }

    @Test
    fun registro_conTokenObtenido_llamaAIngresarConGoogle() = runTest {
        val vm = registro()

        vm.continuarConGoogle(Resultado.Exito("token-2"))

        assertEquals(listOf("token-2"), auth.llamadasIngresarConGoogle)
        assertFalse(vm.uiState.value.enviando)
    }

    @Test
    fun registro_sinConexion_bloqueaYNoLlama() = runTest {
        conectividad.establecer(Conectividad.SIN_CONEXION)
        val vm = registro()

        vm.continuarConGoogle(Resultado.Exito("token-2"))

        assertTrue(auth.llamadasIngresarConGoogle.isEmpty())
        assertEquals(ErrorApp.SinConexion, vm.uiState.value.errorEnvio)
    }

    @Test
    fun registro_conCuentaExistenteConOtroProveedor_abreLaVinculacionYLaCompleta() = runTest {
        auth.resultadoIngresarConGoogle = Resultado.Fallo(ErrorApp.CuentaExistenteConOtroProveedor)
        val vm = registro()
        vm.onEmailChange("ana@mail.com")

        vm.continuarConGoogle(Resultado.Exito("token-2"))
        assertEquals("token-2", vm.uiState.value.vinculacion?.idToken)

        vm.onVinculacionPasswordChange("Secreto123")
        vm.confirmarVinculacion()

        assertEquals(listOf("ana@mail.com" to "Secreto123"), auth.llamadasIngresar)
        assertEquals(listOf("token-2"), auth.llamadasVincularGoogle)
        assertNull(vm.uiState.value.vinculacion)
    }
}
