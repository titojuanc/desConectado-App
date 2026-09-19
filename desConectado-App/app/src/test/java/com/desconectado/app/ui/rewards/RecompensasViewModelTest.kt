package com.desconectado.app.ui.rewards

import com.desconectado.app.domain.model.Conectividad
import com.desconectado.app.domain.model.ErrorApp
import com.desconectado.app.domain.model.Recompensa
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.model.TipoRecompensa
import com.desconectado.app.fakes.FakeCatalogRepository
import com.desconectado.app.fakes.FakeConnectivityMonitor
import com.desconectado.app.testutil.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RecompensasViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val catalogo = FakeCatalogRepository()
    private val conectividad = FakeConnectivityMonitor()

    private val recompensas = listOf(
        Recompensa("r2", "Tema de color", "Un tema para la app.", 100, TipoRecompensa.TEMA, order = 2),
        Recompensa("r1", "Primer paso", "Tu primera insignia.", 50, TipoRecompensa.INSIGNIA, order = 1),
    )

    private fun crearViewModel() = RecompensasViewModel(catalogo, conectividad)

    @Test
    fun cargaYOrdenaPorOrder() = runTest {
        val compuerta = CompletableDeferred<Unit>()
        catalogo.compuerta = compuerta
        catalogo.resultadoRecompensas = Resultado.Exito(recompensas)

        val vm = crearViewModel()
        assertEquals(RecompensasUiState.Cargando, vm.estado.value)

        compuerta.complete(Unit)
        assertEquals(RecompensasUiState.Lista(recompensas.sortedBy { it.order }), vm.estado.value)
        assertEquals(listOf("r1", "r2"), (vm.estado.value as RecompensasUiState.Lista).recompensas.map { it.id })
    }

    @Test
    fun unFallo_produceErrorYReintentarVuelveAPedir() = runTest {
        catalogo.resultadoRecompensas = Resultado.Fallo(ErrorApp.Desconocido)
        val vm = crearViewModel()
        assertEquals(RecompensasUiState.Error, vm.estado.value)

        catalogo.resultadoRecompensas = Resultado.Exito(recompensas)
        vm.reintentar()

        assertEquals(2, catalogo.llamadasRecompensas)
        assertEquals(RecompensasUiState.Lista(recompensas.sortedBy { it.order }), vm.estado.value)
    }

    @Test
    fun sinConexionDelRepositorio_produceElEstadoSinConexion() = runTest {
        catalogo.resultadoRecompensas = Resultado.Fallo(ErrorApp.SinConexion)

        val vm = crearViewModel()

        assertEquals(RecompensasUiState.SinConexion, vm.estado.value)
    }

    @Test
    fun sinConexionDelDispositivo_noCarga() = runTest {
        conectividad.establecer(Conectividad.SIN_CONEXION)

        val vm = crearViewModel()

        assertEquals(RecompensasUiState.SinConexion, vm.estado.value)
        assertEquals(0, catalogo.llamadasRecompensas)
    }

    @Test
    fun alVolverLaConexion_cargaSola() = runTest {
        conectividad.establecer(Conectividad.SIN_CONEXION)
        catalogo.resultadoRecompensas = Resultado.Exito(recompensas)
        val vm = crearViewModel()

        conectividad.establecer(Conectividad.CONECTADO)

        assertEquals(RecompensasUiState.Lista(recompensas.sortedBy { it.order }), vm.estado.value)
    }
}
