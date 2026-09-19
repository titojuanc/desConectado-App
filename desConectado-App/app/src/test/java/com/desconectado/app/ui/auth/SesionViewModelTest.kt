package com.desconectado.app.ui.auth

import com.desconectado.app.domain.model.Conectividad
import com.desconectado.app.domain.model.EstadoSesion
import com.desconectado.app.fakes.FakeAuthRepository
import com.desconectado.app.fakes.FakeConnectivityMonitor
import com.desconectado.app.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SesionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val conectividad = FakeConnectivityMonitor()

    @Test
    fun elEstadoDelRepositorioSeReflejaEnElViewModel() = runTest {
        val auth = FakeAuthRepository(EstadoSesion.Cargando)
        val vm = SesionViewModel(auth, conectividad)
        assertEquals(EstadoSesion.Cargando, vm.estado.value)

        auth.establecerEstado(EstadoSesion.SinSesion)
        assertEquals(EstadoSesion.SinSesion, vm.estado.value)

        auth.establecerEstado(EstadoSesion.ConSesion("uid-1"))
        assertEquals(EstadoSesion.ConSesion("uid-1"), vm.estado.value)
    }

    @Test
    fun alIniciarConConexion_verificaLaCuenta() = runTest {
        val auth = FakeAuthRepository(EstadoSesion.ConSesion("uid-1"))

        SesionViewModel(auth, conectividad)

        assertEquals(1, auth.verificaciones)
    }

    @Test
    fun alIniciarSinConexion_noVerificaLaCuentaYLaSesionSigue() = runTest {
        conectividad.establecer(Conectividad.SIN_CONEXION)
        val auth = FakeAuthRepository(EstadoSesion.ConSesion("uid-1"))

        val vm = SesionViewModel(auth, conectividad)

        assertEquals(0, auth.verificaciones)
        assertEquals(0, auth.cierresDeSesion)
        // La sesión no se cierra por no tener conexión (FR-011).
        assertEquals(EstadoSesion.ConSesion("uid-1"), vm.estado.value)
    }

    @Test
    fun alVolverLaConexion_verificaLaCuentaUnaSolaVez() = runTest {
        conectividad.establecer(Conectividad.SIN_CONEXION)
        val auth = FakeAuthRepository(EstadoSesion.ConSesion("uid-1"))
        SesionViewModel(auth, conectividad)
        assertEquals(0, auth.verificaciones)

        conectividad.establecer(Conectividad.CONECTADO)
        conectividad.establecer(Conectividad.SIN_CONEXION)
        conectividad.establecer(Conectividad.CONECTADO)

        assertEquals(1, auth.verificaciones)
    }
}
