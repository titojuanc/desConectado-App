package com.desconectado.app.ui.points

import com.desconectado.app.domain.model.Conectividad
import com.desconectado.app.domain.model.ErrorApp
import com.desconectado.app.domain.model.EstadoSesion
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.fakes.FakeAuthRepository
import com.desconectado.app.fakes.FakeConnectivityMonitor
import com.desconectado.app.fakes.FakePointsRepository
import com.desconectado.app.fakes.FakeProfileRepository
import com.desconectado.app.testutil.MainDispatcherRule
import com.desconectado.app.ui.profile.PerfilUiState
import com.desconectado.app.ui.profile.PerfilViewModel
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SaldoViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository(EstadoSesion.ConSesion("uid-1"))
    private val perfiles = FakeProfileRepository()
    private val puntos = FakePointsRepository()
    private val conectividad = FakeConnectivityMonitor()

    private fun crearViewModel() = SaldoViewModel(auth, puntos, conectividad)

    @Test
    fun cargaElSaldoProcesadoDeLaSesion() = runTest {
        puntos.saldoResultado = Resultado.Exito(120)

        val vm = crearViewModel()

        assertEquals(SaldoUiState.Disponible(120), vm.estado.value)
        assertEquals(listOf("uid-1"), puntos.llamadasSaldo)
    }

    @Test
    fun unaCuentaNuevaMuestraSaldo0() = runTest {
        puntos.saldoResultado = Resultado.Exito(0)

        val vm = crearViewModel()

        assertEquals(SaldoUiState.Disponible(0), vm.estado.value)
    }

    @Test
    fun unFalloOSinConexionDelRepositorio_noMuestraUn0Inventado() = runTest {
        puntos.saldoResultado = Resultado.Fallo(ErrorApp.Desconocido)
        assertEquals(SaldoUiState.NoDisponible, crearViewModel().estado.value)

        puntos.saldoResultado = Resultado.Fallo(ErrorApp.SinConexion)
        assertEquals(SaldoUiState.NoDisponible, crearViewModel().estado.value)
    }

    @Test
    fun sinConexionDelDispositivo_noCargaYNoMuestraSaldo() = runTest {
        conectividad.establecer(Conectividad.SIN_CONEXION)

        val vm = crearViewModel()

        assertEquals(SaldoUiState.NoDisponible, vm.estado.value)
        assertEquals(emptyList<String>(), puntos.llamadasSaldo)
    }

    @Test
    fun alVolverLaConexion_cargaSolo() = runTest {
        conectividad.establecer(Conectividad.SIN_CONEXION)
        puntos.saldoResultado = Resultado.Exito(70)
        val vm = crearViewModel()

        conectividad.establecer(Conectividad.CONECTADO)

        assertEquals(SaldoUiState.Disponible(70), vm.estado.value)
    }

    @Test
    fun recargar_vuelveAPedirElSaldo() = runTest {
        puntos.saldoResultado = Resultado.Fallo(ErrorApp.Desconocido)
        val vm = crearViewModel()
        assertEquals(SaldoUiState.NoDisponible, vm.estado.value)

        puntos.saldoResultado = Resultado.Exito(30)
        vm.recargar()

        assertEquals(2, puntos.llamadasSaldo.size)
        assertEquals(SaldoUiState.Disponible(30), vm.estado.value)
    }

    @Test
    fun alCerrarSesion_noQuedaElSaldoDeLaPersonaAnterior() = runTest {
        puntos.saldoResultado = Resultado.Exito(500)
        val vm = crearViewModel()
        assertEquals(SaldoUiState.Disponible(500), vm.estado.value)

        auth.cerrarSesion()

        assertEquals(SaldoUiState.Cargando, vm.estado.value)
    }

    @Test
    fun unFalloQueNoEsDeConexion_seReintentaSoloParaNoDejarElGuion() = runTest {
        // Primer ingreso con Google: la sesión empieza antes de que exista el perfil.
        puntos.saldoResultado = Resultado.Fallo(ErrorApp.Desconocido)
        val vm = crearViewModel()
        assertEquals(SaldoUiState.NoDisponible, vm.estado.value)

        puntos.saldoResultado = Resultado.Exito(0)
        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(2_500)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertEquals(SaldoUiState.Disponible(0), vm.estado.value)
    }

    @Test
    fun elReintentoAutomaticoEstaAcotado() = runTest {
        puntos.saldoResultado = Resultado.Fallo(ErrorApp.Desconocido)
        val vm = crearViewModel()

        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(60_000)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertEquals(3, puntos.llamadasSaldo.size) // el intento inicial y 2 reintentos
        assertEquals(SaldoUiState.NoDisponible, vm.estado.value)
    }

    @Test
    fun sinConexionNoSeReintentaSolo() = runTest {
        puntos.saldoResultado = Resultado.Fallo(ErrorApp.SinConexion)
        crearViewModel()

        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(60_000)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertEquals(1, puntos.llamadasSaldo.size)
    }

    @Test
    fun elSaldoDelIndicadorCoincideConElDelPerfil() = runTest {
        perfiles.resultadoPerfil = Resultado.Exito(com.desconectado.app.domain.model.Perfil(username = "Ana", email = "ana@mail.com", puntos = 340))
        puntos.saldoResultado = Resultado.Exito(340)

        val saldo = crearViewModel()
        val perfil = PerfilViewModel(
            auth,
            perfiles,
            com.desconectado.app.fakes.FakeChallengeRepository(),
            com.desconectado.app.fakes.FakeAchievementRepository(),
            puntos,
            conectividad,
        )

        val enPerfil = (perfil.estado.value as PerfilUiState.Datos).perfil.puntos
        assertEquals(SaldoUiState.Disponible(enPerfil), saldo.estado.value)
    }
}
