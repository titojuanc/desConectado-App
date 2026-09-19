package com.desconectado.app.ui.rewards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desconectado.app.domain.model.Conectividad
import com.desconectado.app.domain.model.ErrorApp
import com.desconectado.app.domain.model.Recompensa
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.repository.CatalogRepository
import com.desconectado.app.domain.repository.ConnectivityMonitor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RecompensasUiState {
    data object Cargando : RecompensasUiState
    data class Lista(val recompensas: List<Recompensa>) : RecompensasUiState
    data object Error : RecompensasUiState
    data object SinConexion : RecompensasUiState
}

class RecompensasViewModel(
    private val catalogo: CatalogRepository,
    conectividad: ConnectivityMonitor,
) : ViewModel() {

    private val _estado = MutableStateFlow<RecompensasUiState>(RecompensasUiState.Cargando)
    val estado: StateFlow<RecompensasUiState> = _estado.asStateFlow()

    private var conectado = true
    private var carga: Job? = null

    init {
        viewModelScope.launch {
            conectividad.estado.collect { valor ->
                conectado = valor == Conectividad.CONECTADO
                // Una lista ya cargada se conserva aunque se pierda la red; el aviso de la barra
                // superior informa. Solo se reacciona si todavía no hay lista.
                if (_estado.value !is RecompensasUiState.Lista) cargar()
            }
        }
    }

    fun reintentar() = cargar()

    private fun cargar() {
        carga?.cancel()
        if (!conectado) {
            _estado.value = RecompensasUiState.SinConexion
            return
        }
        _estado.value = RecompensasUiState.Cargando
        carga = viewModelScope.launch {
            _estado.value = when (val resultado = catalogo.recompensas()) {
                is Resultado.Exito -> RecompensasUiState.Lista(resultado.valor.sortedBy { it.order })
                is Resultado.Fallo ->
                    if (resultado.error == ErrorApp.SinConexion) RecompensasUiState.SinConexion else RecompensasUiState.Error
            }
        }
    }
}
