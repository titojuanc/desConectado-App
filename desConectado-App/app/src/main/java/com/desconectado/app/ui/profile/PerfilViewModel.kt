package com.desconectado.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desconectado.app.domain.model.Conectividad
import com.desconectado.app.domain.model.ErrorApp
import com.desconectado.app.domain.model.EstadoSesion
import com.desconectado.app.domain.model.Perfil
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.repository.AuthRepository
import com.desconectado.app.domain.repository.ConnectivityMonitor
import com.desconectado.app.domain.repository.ProfileRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

sealed interface PerfilUiState {
    data object Cargando : PerfilUiState
    data class Datos(val perfil: Perfil) : PerfilUiState
    data object Error : PerfilUiState
    data object SinConexion : PerfilUiState
}

class PerfilViewModel(
    private val auth: AuthRepository,
    private val perfiles: ProfileRepository,
    conectividad: ConnectivityMonitor,
) : ViewModel() {

    private val _estado = MutableStateFlow<PerfilUiState>(PerfilUiState.Cargando)
    val estado: StateFlow<PerfilUiState> = _estado.asStateFlow()

    private var uid: String? = null
    private var conectado = true
    private var carga: Job? = null

    init {
        viewModelScope.launch {
            combine(auth.authState, conectividad.estado) { sesion, red -> sesion to red }
                .collect { (sesion, red) ->
                    conectado = red == Conectividad.CONECTADO
                    when (sesion) {
                        is EstadoSesion.ConSesion -> when {
                            sesion.uid != uid -> {
                                uid = sesion.uid
                                cargar()
                            }
                            _estado.value is PerfilUiState.Error || _estado.value is PerfilUiState.SinConexion -> cargar()
                        }
                        // Al cerrar sesión no queda ningún dato de la persona anterior.
                        EstadoSesion.SinSesion -> {
                            carga?.cancel()
                            uid = null
                            _estado.value = PerfilUiState.Cargando
                        }
                        EstadoSesion.Cargando -> Unit
                    }
                }
        }
    }

    fun reintentar() = cargar()

    fun cerrarSesion() = auth.cerrarSesion()

    private fun cargar() {
        val uidActual = uid ?: return
        carga?.cancel()
        if (!conectado) {
            _estado.value = PerfilUiState.SinConexion
            return
        }
        _estado.value = PerfilUiState.Cargando
        carga = viewModelScope.launch {
            _estado.value = when (val resultado = perfiles.perfil(uidActual)) {
                is Resultado.Exito -> PerfilUiState.Datos(resultado.valor)
                is Resultado.Fallo ->
                    if (resultado.error == ErrorApp.SinConexion) PerfilUiState.SinConexion else PerfilUiState.Error
            }
        }
    }
}
