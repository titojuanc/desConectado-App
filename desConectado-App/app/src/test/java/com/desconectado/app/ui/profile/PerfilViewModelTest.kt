package com.desconectado.app.ui.profile

import com.desconectado.app.domain.model.Conectividad
import com.desconectado.app.domain.model.DesafioHecho
import com.desconectado.app.domain.model.ErrorApp
import com.desconectado.app.domain.model.EstadoSesion
import com.desconectado.app.domain.model.Perfil
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.fakes.FakeAuthRepository
import com.desconectado.app.fakes.FakeConnectivityMonitor
import com.desconectado.app.fakes.FakePointsRepository
import com.desconectado.app.fakes.FakeProfileRepository
import com.desconectado.app.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class PerfilViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository(EstadoSesion.ConSesion("uid-1"))
    private val perfiles = FakeProfileRepository()
    private val puntos = FakePointsRepository()
    private val conectividad = FakeConnectivityMonitor()

    private val perfilAna = Perfil(username = "Ana Prueba", email = "ana@mail.com")

    private val desafiosHechos = listOf(
        DesafioHecho("Salir a trotar", 50, Instant.parse("2026-09-22T10:00:00Z")),
        DesafioHecho("Salir a caminar", 10, Instant.parse("2026-09-20T10:00:00Z")),
    )

    private fun crearViewModel() = PerfilViewModel(auth, perfiles, puntos, conectividad)

    @Test
    fun cargaElPerfilDeLaSesionActualYExponeNombreYCorreo() = runTest {
        perfiles.resultadoPerfil = Resultado.Exito(perfilAna)

        val vm = crearViewModel()

        assertEquals(PerfilUiState.Datos(perfilAna), vm.estado.value)
        assertEquals(listOf("uid-1"), perfiles.llamadasPerfil)
    }

    @Test
    fun unFallo_produceElEstadoDeErrorYReintentarVuelveAPedir() = runTest {
        perfiles.resultadoPerfil = Resultado.Fallo(ErrorApp.Desconocido)
        val vm = crearViewModel()
        assertEquals(PerfilUiState.Error, vm.estado.value)

        perfiles.resultadoPerfil = Resultado.Exito(perfilAna)
        vm.reintentar()

        assertEquals(2, perfiles.llamadasPerfil.size)
        assertEquals(PerfilUiState.Datos(perfilAna), vm.estado.value)
    }

    @Test
    fun sinConexionDelRepositorio_produceElEstadoSinConexion() = runTest {
        perfiles.resultadoPerfil = Resultado.Fallo(ErrorApp.SinConexion)

        val vm = crearViewModel()

        assertEquals(PerfilUiState.SinConexion, vm.estado.value)
    }

    @Test
    fun sinConexionDelDispositivo_noCargaYMuestraSinConexion() = runTest {
        conectividad.establecer(Conectividad.SIN_CONEXION)

        val vm = crearViewModel()

        assertEquals(PerfilUiState.SinConexion, vm.estado.value)
        assertEquals(emptyList<String>(), perfiles.llamadasPerfil)
    }

    @Test
    fun alVolverLaConexion_cargaSolo() = runTest {
        conectividad.establecer(Conectividad.SIN_CONEXION)
        perfiles.resultadoPerfil = Resultado.Exito(perfilAna)
        val vm = crearViewModel()

        conectividad.establecer(Conectividad.CONECTADO)

        assertEquals(PerfilUiState.Datos(perfilAna), vm.estado.value)
    }

    @Test
    fun cerrarSesion_llamaAlRepositorio() = runTest {
        val vm = crearViewModel()

        vm.cerrarSesion()

        assertEquals(1, auth.cierresDeSesion)
    }

    @Test
    fun alCambiarDePersona_noQuedanDatosDeLaAnterior() = runTest {
        perfiles.resultadoPerfil = Resultado.Exito(perfilAna)
        val vm = crearViewModel()
        assertEquals(PerfilUiState.Datos(perfilAna), vm.estado.value)

        auth.cerrarSesion()
        assertEquals(PerfilUiState.Cargando, vm.estado.value)

        val perfilBeto = Perfil(username = "Beto", email = "beto@mail.com")
        perfiles.resultadoPerfil = Resultado.Exito(perfilBeto)
        auth.establecerEstado(EstadoSesion.ConSesion("uid-2"))

        assertEquals(PerfilUiState.Datos(perfilBeto), vm.estado.value)
        assertEquals(listOf("uid-1", "uid-2"), perfiles.llamadasPerfil)
    }

    // --- Puntos y desafíos hechos (FR-027 a FR-033) ---

    @Test
    fun exponeElSaldoDeLaPersonaJuntoConSusDatos() = runTest {
        val conPuntos = perfilAna.copy(puntos = 340)
        perfiles.resultadoPerfil = Resultado.Exito(conPuntos)

        val vm = crearViewModel()

        assertEquals(PerfilUiState.Datos(conPuntos), vm.estado.value)
    }

    @Test
    fun cargaLosUltimosDesafiosHechosPidiendoCinco() = runTest {
        puntos.resultado = Resultado.Exito(desafiosHechos)

        val vm = crearViewModel()

        assertEquals(DesafiosHechosUiState.Lista(desafiosHechos), vm.desafiosHechos.value)
        assertEquals(listOf("uid-1" to 5), puntos.llamadas)
    }

    @Test
    fun unaCuentaNuevaTieneListaVaciaYSaldo0SinSerUnError() = runTest {
        perfiles.resultadoPerfil = Resultado.Exito(perfilAna)
        puntos.resultado = Resultado.Exito(emptyList())

        val vm = crearViewModel()

        assertEquals(DesafiosHechosUiState.Lista(emptyList()), vm.desafiosHechos.value)
        assertEquals(PerfilUiState.Datos(perfilAna), vm.estado.value)
        assertEquals(0, perfilAna.puntos)
    }

    @Test
    fun unFalloDePuntos_dejaErrorConReintentoSinPerderNombreNiCorreo() = runTest {
        perfiles.resultadoPerfil = Resultado.Exito(perfilAna)
        puntos.resultado = Resultado.Fallo(ErrorApp.Desconocido)

        val vm = crearViewModel()

        assertEquals(DesafiosHechosUiState.Error, vm.desafiosHechos.value)
        assertEquals(PerfilUiState.Datos(perfilAna), vm.estado.value)
    }

    @Test
    fun sinConexionEnPuntos_muestraSinConexionYNuncaUnaListaVacia() = runTest {
        puntos.resultado = Resultado.Fallo(ErrorApp.SinConexion)

        val vm = crearViewModel()

        assertEquals(DesafiosHechosUiState.SinConexion, vm.desafiosHechos.value)
    }

    @Test
    fun sinConexionDelDispositivo_noPideLosDesafiosHechos() = runTest {
        conectividad.establecer(Conectividad.SIN_CONEXION)

        val vm = crearViewModel()

        assertEquals(DesafiosHechosUiState.SinConexion, vm.desafiosHechos.value)
        assertEquals(emptyList<Pair<String, Int>>(), puntos.llamadas)
    }

    @Test
    fun reintentarPuntos_vuelveAPedirSoloLosDesafiosHechos() = runTest {
        puntos.resultado = Resultado.Fallo(ErrorApp.Desconocido)
        val vm = crearViewModel()
        assertEquals(DesafiosHechosUiState.Error, vm.desafiosHechos.value)

        puntos.resultado = Resultado.Exito(desafiosHechos)
        vm.reintentarPuntos()

        assertEquals(2, puntos.llamadas.size)
        assertEquals(1, perfiles.llamadasPerfil.size)
        assertEquals(DesafiosHechosUiState.Lista(desafiosHechos), vm.desafiosHechos.value)
    }

    @Test
    fun alCerrarSesion_noQuedanDesafiosDeLaPersonaAnterior() = runTest {
        puntos.resultado = Resultado.Exito(desafiosHechos)
        val vm = crearViewModel()
        assertEquals(DesafiosHechosUiState.Lista(desafiosHechos), vm.desafiosHechos.value)

        auth.cerrarSesion()

        assertEquals(DesafiosHechosUiState.Cargando, vm.desafiosHechos.value)
    }
}
