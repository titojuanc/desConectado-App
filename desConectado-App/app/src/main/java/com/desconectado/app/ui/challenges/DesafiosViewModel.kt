package com.desconectado.app.ui.challenges

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desconectado.app.domain.model.Conectividad
import com.desconectado.app.domain.model.Desafio
import com.desconectado.app.domain.model.ErrorApp
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.repository.CatalogRepository
import com.desconectado.app.domain.repository.ConnectivityMonitor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DesafiosUiState {
    data object Cargando : DesafiosUiState
    data class Lista(val desafios: List<Desafio>) : DesafiosUiState
    data object Error : DesafiosUiState
    data object SinConexion : DesafiosUiState
}

class DesafiosViewModel(
    private val catalogo: CatalogRepository,
    conectividad: ConnectivityMonitor,
) : ViewModel() {

    private val _estado = MutableStateFlow<DesafiosUiState>(DesafiosUiState.Cargando)
    val estado: StateFlow<DesafiosUiState> = _estado.asStateFlow()

    private var conectado = true
    private var carga: Job? = null

    init {
        viewModelScope.launch {
            conectividad.estado.collect { valor ->
                conectado = valor == Conectividad.CONECTADO
                // Una lista ya cargada se conserva aunque se pierda la red; el aviso de la barra
                // superior informa. Solo se reacciona si todavía no hay lista.
                if (_estado.value !is DesafiosUiState.Lista) cargar()
            }
        }
    }

    fun reintentar() = cargar()

    private fun cargar() {
        carga?.cancel()
        if (!conectado) {
            _estado.value = DesafiosUiState.SinConexion
            return
        }
        _estado.value = DesafiosUiState.Cargando
        carga = viewModelScope.launch {
            _estado.value = when (val resultado = catalogo.desafios()) {
                is Resultado.Exito -> DesafiosUiState.Lista(resultado.valor.sortedBy { it.order })
                is Resultado.Fallo ->
                    if (resultado.error == ErrorApp.SinConexion) DesafiosUiState.SinConexion else DesafiosUiState.Error
            }
        }
    }
}
