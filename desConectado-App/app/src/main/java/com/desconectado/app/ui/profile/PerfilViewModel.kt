package com.desconectado.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desconectado.app.domain.model.Conectividad
import com.desconectado.app.domain.model.DesafioHecho
import com.desconectado.app.domain.model.ErrorApp
import com.desconectado.app.domain.model.EstadoSesion
import com.desconectado.app.domain.model.Perfil
import com.desconectado.app.domain.model.RedeemedReward
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.repository.AuthRepository
import com.desconectado.app.domain.repository.ConnectivityMonitor
import com.desconectado.app.domain.repository.ChallengeRepository
import com.desconectado.app.domain.repository.ProfileRepository
import com.desconectado.app.domain.repository.PointsRepository
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

/** Historial de desafíos. Falla por separado del perfil: no oculta nombre ni correo. */
sealed interface DesafiosHechosUiState {
    data object Cargando : DesafiosHechosUiState

    /** Vacía si todavía no hay desafíos hechos: no es un error. */
    data class Lista(val desafios: List<DesafioHecho>) : DesafiosHechosUiState

    data object Error : DesafiosHechosUiState
    data object SinConexion : DesafiosHechosUiState
}

sealed interface CanjesPerfilUiState {
    data object Cargando : CanjesPerfilUiState
    data class Lista(val canjes: List<RedeemedReward>) : CanjesPerfilUiState
    data object Error : CanjesPerfilUiState
    data object SinConexion : CanjesPerfilUiState
}

class PerfilViewModel(
    private val auth: AuthRepository,
    private val perfiles: ProfileRepository,
    private val desafios: ChallengeRepository,
    private val puntos: PointsRepository,
    conectividad: ConnectivityMonitor,
) : ViewModel() {

    private val _estado = MutableStateFlow<PerfilUiState>(PerfilUiState.Cargando)
    val estado: StateFlow<PerfilUiState> = _estado.asStateFlow()

    private val _desafiosHechos = MutableStateFlow<DesafiosHechosUiState>(DesafiosHechosUiState.Cargando)
    val desafiosHechos: StateFlow<DesafiosHechosUiState> = _desafiosHechos.asStateFlow()

    private val _canjes = MutableStateFlow<CanjesPerfilUiState>(CanjesPerfilUiState.Cargando)
    val canjes: StateFlow<CanjesPerfilUiState> = _canjes.asStateFlow()

    private var uid: String? = null
    private var conectado = true
    private var carga: Job? = null
    private var cargaDesafios: Job? = null
    private var cargaCanjes: Job? = null

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
                            _desafiosHechos.value.necesitaReintento() -> cargarDesafiosHechos()
                            _canjes.value.necesitaReintento() -> cargarCanjes()
                        }
                        // Al cerrar sesión no queda ningún dato de la persona anterior.
                        EstadoSesion.SinSesion -> {
                            carga?.cancel()
                            cargaDesafios?.cancel()
                            cargaCanjes?.cancel()
                            uid = null
                            _estado.value = PerfilUiState.Cargando
                            _desafiosHechos.value = DesafiosHechosUiState.Cargando
                            _canjes.value = CanjesPerfilUiState.Cargando
                        }
                        EstadoSesion.Cargando -> Unit
                    }
                }
        }
    }

    fun reintentar() = cargar()

    fun reintentarHistorial() = cargarDesafiosHechos()

    fun reintentarCanjes() = cargarCanjes()

    fun cerrarSesion() = auth.cerrarSesion()

    private fun DesafiosHechosUiState.necesitaReintento() =
        this is DesafiosHechosUiState.Error || this is DesafiosHechosUiState.SinConexion

    private fun CanjesPerfilUiState.necesitaReintento() =
        this is CanjesPerfilUiState.Error || this is CanjesPerfilUiState.SinConexion

    private fun cargarDesafiosHechos() {
        val uidActual = uid ?: return
        cargaDesafios?.cancel()
        if (!conectado) {
            _desafiosHechos.value = DesafiosHechosUiState.SinConexion
            return
        }
        _desafiosHechos.value = DesafiosHechosUiState.Cargando
        cargaDesafios = viewModelScope.launch {
            _desafiosHechos.value = when (val resultado = desafios.history(uidActual)) {
                is Resultado.Exito -> DesafiosHechosUiState.Lista(resultado.valor)
                is Resultado.Fallo ->
                    if (resultado.error == ErrorApp.SinConexion) DesafiosHechosUiState.SinConexion else DesafiosHechosUiState.Error
            }
        }
    }

    private fun cargarCanjes() {
        val uidActual = uid ?: return
        cargaCanjes?.cancel()
        if (!conectado) {
            _canjes.value = CanjesPerfilUiState.SinConexion
            return
        }
        _canjes.value = CanjesPerfilUiState.Cargando
        cargaCanjes = viewModelScope.launch {
            _canjes.value = when (val resultado = puntos.recompensasCanjeadas(uidActual)) {
                is Resultado.Exito -> CanjesPerfilUiState.Lista(resultado.valor)
                is Resultado.Fallo ->
                    if (resultado.error == ErrorApp.SinConexion) CanjesPerfilUiState.SinConexion else CanjesPerfilUiState.Error
            }
        }
    }

    private fun cargar() {
        val uidActual = uid ?: return
        carga?.cancel()
        cargarDesafiosHechos()
        cargarCanjes()
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
