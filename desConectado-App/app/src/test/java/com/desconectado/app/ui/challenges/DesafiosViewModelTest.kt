package com.desconectado.app.ui.challenges

import com.desconectado.app.domain.model.Conectividad
import com.desconectado.app.domain.model.Desafio
import com.desconectado.app.domain.model.Dificultad
import com.desconectado.app.domain.model.ErrorApp
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.fakes.FakeCatalogRepository
import com.desconectado.app.fakes.FakeConnectivityMonitor
import com.desconectado.app.testutil.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DesafiosViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val catalogo = FakeCatalogRepository()
    private val conectividad = FakeConnectivityMonitor()

    private val desafios = listOf(
        Desafio("d2", "No uses redes sociales por 1 hora", "Una hora sin redes.", 60, Dificultad.FACIL, 20, order = 2),
        Desafio("d1", "No uses redes sociales por 30 minutos", "Media hora sin redes.", 30, Dificultad.FACIL, 10, order = 1),
    )

    private fun crearViewModel() = DesafiosViewModel(catalogo, conectividad)

    @Test
    fun pasaDeCargandoALista() = runTest {
        val compuerta = CompletableDeferred<Unit>()
        catalogo.compuerta = compuerta
        catalogo.resultadoDesafios = Resultado.Exito(desafios)

        val vm = crearViewModel()
        assertEquals(DesafiosUiState.Cargando, vm.estado.value)

        compuerta.complete(Unit)
        assertEquals(DesafiosUiState.Lista(desafios.sortedBy { it.order }), vm.estado.value)
    }

    @Test
    fun unFallo_produceElEstadoDeErrorConReintento() = runTest {
        catalogo.resultadoDesafios = Resultado.Fallo(ErrorApp.Desconocido)

        val vm = crearViewModel()

        assertEquals(DesafiosUiState.Error, vm.estado.value)
    }

    @Test
    fun sinConexionDelRepositorio_produceElEstadoSinConexion() = runTest {
        catalogo.resultadoDesafios = Resultado.Fallo(ErrorApp.SinConexion)

        val vm = crearViewModel()

        assertEquals(DesafiosUiState.SinConexion, vm.estado.value)
    }

    @Test
    fun sinConexionDelDispositivo_muestraElEstadoSinConexionYNoCarga() = runTest {
        conectividad.establecer(Conectividad.SIN_CONEXION)

        val vm = crearViewModel()

        assertEquals(DesafiosUiState.SinConexion, vm.estado.value)
        assertEquals(0, catalogo.llamadasDesafios)
    }

    @Test
    fun alVolverLaConexion_cargaSola() = runTest {
        conectividad.establecer(Conectividad.SIN_CONEXION)
        catalogo.resultadoDesafios = Resultado.Exito(desafios)
        val vm = crearViewModel()
        assertEquals(DesafiosUiState.SinConexion, vm.estado.value)

        conectividad.establecer(Conectividad.CONECTADO)

        assertEquals(DesafiosUiState.Lista(desafios.sortedBy { it.order }), vm.estado.value)
    }

    @Test
    fun reintentar_vuelveAPedirLosDesafios() = runTest {
        catalogo.resultadoDesafios = Resultado.Fallo(ErrorApp.Desconocido)
        val vm = crearViewModel()
        assertEquals(DesafiosUiState.Error, vm.estado.value)
        assertEquals(1, catalogo.llamadasDesafios)

        catalogo.resultadoDesafios = Resultado.Exito(desafios)
        vm.reintentar()

        assertEquals(2, catalogo.llamadasDesafios)
        assertEquals(DesafiosUiState.Lista(desafios.sortedBy { it.order }), vm.estado.value)
    }

    @Test
    fun conLaListaCargada_perderLaConexionNoLaReemplaza() = runTest {
        catalogo.resultadoDesafios = Resultado.Exito(desafios)
        val vm = crearViewModel()

        conectividad.establecer(Conectividad.SIN_CONEXION)

        assertEquals(DesafiosUiState.Lista(desafios.sortedBy { it.order }), vm.estado.value)
    }
}
