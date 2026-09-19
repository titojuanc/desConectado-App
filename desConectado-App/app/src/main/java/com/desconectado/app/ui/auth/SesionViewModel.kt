package com.desconectado.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desconectado.app.domain.model.Conectividad
import com.desconectado.app.domain.model.EstadoSesion
import com.desconectado.app.domain.repository.AuthRepository
import com.desconectado.app.domain.repository.ConnectivityMonitor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Estado de la sesión para toda la app. Verifica la cuenta una sola vez, en cuanto hay conexión:
 * abrir la app sin conexión nunca cierra la sesión (FR-011); la verificación queda pendiente hasta
 * que vuelva la red.
 */
class SesionViewModel(
    private val auth: AuthRepository,
    conectividad: ConnectivityMonitor,
) : ViewModel() {

    val estado: StateFlow<EstadoSesion> = auth.authState
        .stateIn(viewModelScope, SharingStarted.Eagerly, EstadoSesion.Cargando)

    init {
        viewModelScope.launch {
            conectividad.estado.first { it == Conectividad.CONECTADO }
            auth.verificarCuenta()
        }
    }
}
