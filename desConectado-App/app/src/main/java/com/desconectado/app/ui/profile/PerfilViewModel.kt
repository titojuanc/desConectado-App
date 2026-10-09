package com.desconectado.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desconectado.app.domain.model.Conectividad
import com.desconectado.app.domain.model.AchievementProgress
import com.desconectado.app.domain.model.DesafioHecho
import com.desconectado.app.domain.model.ErrorApp
import com.desconectado.app.domain.model.EstadoSesion
import com.desconectado.app.domain.model.Perfil
import com.desconectado.app.domain.model.RedeemedReward
import com.desconectado.app.domain.ProgressCalculator
import com.desconectado.app.domain.ProgressMetrics
import com.desconectado.app.domain.model.UpcomingPointExpiry
import com.desconectado.app.domain.model.UserPreferences
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.repository.AuthRepository
import com.desconectado.app.domain.repository.AchievementRepository
import com.desconectado.app.domain.repository.ConnectivityMonitor
import com.desconectado.app.domain.repository.ChallengeRepository
import com.desconectado.app.domain.repository.ProfileRepository
import com.desconectado.app.domain.repository.PointsRepository
import com.desconectado.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Clock

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

sealed interface ProximoVencimientoUiState {
    data object Cargando : ProximoVencimientoUiState
    data class Proximo(val vencimiento: UpcomingPointExpiry) : ProximoVencimientoUiState
    data object SinVencimientos : ProximoVencimientoUiState
    data object Error : ProximoVencimientoUiState
    data object SinConexion : ProximoVencimientoUiState
}

sealed interface ProgresoUiState {
    data object Oculto : ProgresoUiState
    data object Cargando : ProgresoUiState
    data class Datos(val metricas: ProgressMetrics, val logros: List<AchievementProgress>) : ProgresoUiState
    data object Error : ProgresoUiState
    data object SinConexion : ProgresoUiState
}

data class EdicionNombreUiState(
    val editando: Boolean = false,
    val texto: String = "",
    val guardando: Boolean = false,
    val error: Boolean = false,
)

sealed interface PreferenciasPerfilUiState {
    data object Oculto : PreferenciasPerfilUiState
    data object Cargando : PreferenciasPerfilUiState
    data class Datos(
        val preferencias: UserPreferences,
        val guardando: Boolean = false,
        val error: Boolean = false,
    ) : PreferenciasPerfilUiState
    data object Error : PreferenciasPerfilUiState
    data object SinConexion : PreferenciasPerfilUiState
}

class PerfilViewModel(
    private val auth: AuthRepository,
    private val perfiles: ProfileRepository,
    private val desafios: ChallengeRepository,
    private val logros: AchievementRepository,
    private val puntos: PointsRepository,
    conectividad: ConnectivityMonitor,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val userPreferences: UserPreferencesRepository? = null,
) : ViewModel() {

    private val _estado = MutableStateFlow<PerfilUiState>(PerfilUiState.Cargando)
    val estado: StateFlow<PerfilUiState> = _estado.asStateFlow()

    private val _desafiosHechos = MutableStateFlow<DesafiosHechosUiState>(DesafiosHechosUiState.Cargando)
    val desafiosHechos: StateFlow<DesafiosHechosUiState> = _desafiosHechos.asStateFlow()

    private val _canjes = MutableStateFlow<CanjesPerfilUiState>(CanjesPerfilUiState.Cargando)
    val canjes: StateFlow<CanjesPerfilUiState> = _canjes.asStateFlow()

    private val _proximoVencimiento = MutableStateFlow<ProximoVencimientoUiState>(ProximoVencimientoUiState.Cargando)
    val proximoVencimiento: StateFlow<ProximoVencimientoUiState> = _proximoVencimiento.asStateFlow()

    private val _progreso = MutableStateFlow<ProgresoUiState>(ProgresoUiState.Cargando)
    val progreso: StateFlow<ProgresoUiState> = _progreso.asStateFlow()

    private val _edicionNombre = MutableStateFlow(EdicionNombreUiState())
    val edicionNombre: StateFlow<EdicionNombreUiState> = _edicionNombre.asStateFlow()

    private val _preferencias = MutableStateFlow<PreferenciasPerfilUiState>(PreferenciasPerfilUiState.Oculto)
    val preferencias: StateFlow<PreferenciasPerfilUiState> = _preferencias.asStateFlow()

    private var uid: String? = null
    private var conectado = true
    private var carga: Job? = null
    private var cargaDesafios: Job? = null
    private var cargaCanjes: Job? = null
    private var cargaVencimiento: Job? = null
    private var cargaProgreso: Job? = null
    private var cargaPreferencias: Job? = null

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
                            _proximoVencimiento.value.necesitaReintento() -> cargarProximoVencimiento()
                            _progreso.value.necesitaReintento() -> cargarProgreso()
                            _preferencias.value.necesitaReintento() -> cargarPreferencias()
                        }
                        // Al cerrar sesión no queda ningún dato de la persona anterior.
                        EstadoSesion.SinSesion -> {
                            carga?.cancel()
                            cargaDesafios?.cancel()
                            cargaCanjes?.cancel()
                            cargaVencimiento?.cancel()
                            cargaProgreso?.cancel()
                            cargaPreferencias?.cancel()
                            uid = null
                            _estado.value = PerfilUiState.Cargando
                            _desafiosHechos.value = DesafiosHechosUiState.Cargando
                            _canjes.value = CanjesPerfilUiState.Cargando
                            _proximoVencimiento.value = ProximoVencimientoUiState.Cargando
                            _progreso.value = ProgresoUiState.Cargando
                            _preferencias.value = PreferenciasPerfilUiState.Oculto
                        }
                        EstadoSesion.Cargando -> Unit
                    }
                }
        }
    }

    fun reintentar() = cargar()

    fun reintentarHistorial() = cargarDesafiosHechos()

    fun reintentarCanjes() = cargarCanjes()

    fun reintentarVencimiento() = cargarProximoVencimiento()

    fun reintentarProgreso() = cargarProgreso()

    fun reintentarPreferencias() = cargarPreferencias()

    fun guardarMetaSemanal(minutos: Int) {
        val actual = _preferencias.value as? PreferenciasPerfilUiState.Datos ?: return
        val uidActual = uid ?: return
        val repository = userPreferences ?: return
        if (minutos !in UserPreferences.META_MINIMA..UserPreferences.META_MAXIMA || minutos % UserPreferences.INCREMENTO_META != 0) {
            _preferencias.value = actual.copy(error = true)
            return
        }
        _preferencias.value = actual.copy(guardando = true, error = false)
        viewModelScope.launch {
            _preferencias.value = when (val result = repository.guardarMetaSemanal(uidActual, minutos)) {
                is Resultado.Exito -> {
                    cargarProgreso()
                    PreferenciasPerfilUiState.Datos(result.valor)
                }
                is Resultado.Fallo -> if (result.error == ErrorApp.SinConexion) {
                    PreferenciasPerfilUiState.SinConexion
                } else {
                    actual.copy(guardando = false, error = true)
                }
            }
        }
    }

    fun configurarNotificaciones(enabled: Boolean) {
        val actual = _preferencias.value as? PreferenciasPerfilUiState.Datos ?: return
        val uidActual = uid ?: return
        val repository = userPreferences ?: return
        _preferencias.value = actual.copy(guardando = true, error = false)
        viewModelScope.launch {
            _preferencias.value = when (val result = repository.configurarNotificaciones(uidActual, enabled)) {
                is Resultado.Exito -> PreferenciasPerfilUiState.Datos(result.valor)
                is Resultado.Fallo -> if (result.error == ErrorApp.SinConexion) {
                    PreferenciasPerfilUiState.SinConexion
                } else {
                    actual.copy(guardando = false, error = true)
                }
            }
        }
    }

    fun editarNombre() {
        val perfil = (_estado.value as? PerfilUiState.Datos)?.perfil ?: return
        _edicionNombre.value = EdicionNombreUiState(editando = true, texto = perfil.username)
    }

    fun cambiarNombre(value: String) {
        _edicionNombre.value = _edicionNombre.value.copy(texto = value, error = false)
    }

    fun cancelarEdicionNombre() {
        _edicionNombre.value = EdicionNombreUiState()
    }

    fun guardarNombre() {
        val current = _edicionNombre.value
        val uidActual = uid ?: return
        val nombre = current.texto.trim()
        if (!current.editando || current.guardando) return
        if (nombre.isEmpty() || nombre.length > 30 || !conectado) {
            _edicionNombre.value = current.copy(error = true)
            return
        }
        _edicionNombre.value = current.copy(guardando = true, error = false)
        viewModelScope.launch {
            when (perfiles.actualizarUsername(uidActual, nombre)) {
                is Resultado.Exito -> {
                    val perfil = (_estado.value as? PerfilUiState.Datos)?.perfil
                    if (perfil != null) _estado.value = PerfilUiState.Datos(perfil.copy(username = nombre))
                    _edicionNombre.value = EdicionNombreUiState()
                }
                is Resultado.Fallo -> _edicionNombre.value = current.copy(guardando = false, error = true)
            }
        }
    }

    fun cerrarSesion() = auth.cerrarSesion()

    private fun DesafiosHechosUiState.necesitaReintento() =
        this is DesafiosHechosUiState.Error || this is DesafiosHechosUiState.SinConexion

    private fun CanjesPerfilUiState.necesitaReintento() =
        this is CanjesPerfilUiState.Error || this is CanjesPerfilUiState.SinConexion

    private fun ProximoVencimientoUiState.necesitaReintento() =
        this is ProximoVencimientoUiState.Error || this is ProximoVencimientoUiState.SinConexion

    private fun ProgresoUiState.necesitaReintento() =
        this is ProgresoUiState.Error || this is ProgresoUiState.SinConexion

    private fun PreferenciasPerfilUiState.necesitaReintento() =
        this is PreferenciasPerfilUiState.Error || this is PreferenciasPerfilUiState.SinConexion

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

    private fun cargarProximoVencimiento() {
        val uidActual = uid ?: return
        cargaVencimiento?.cancel()
        if (!conectado) {
            _proximoVencimiento.value = ProximoVencimientoUiState.SinConexion
            return
        }
        _proximoVencimiento.value = ProximoVencimientoUiState.Cargando
        cargaVencimiento = viewModelScope.launch {
            _proximoVencimiento.value = when (val resultado = puntos.proximoVencimiento(uidActual)) {
                is Resultado.Exito -> resultado.valor?.let(ProximoVencimientoUiState::Proximo)
                    ?: ProximoVencimientoUiState.SinVencimientos
                is Resultado.Fallo -> if (resultado.error == ErrorApp.SinConexion) {
                    ProximoVencimientoUiState.SinConexion
                } else {
                    ProximoVencimientoUiState.Error
                }
            }
        }
    }

    private fun cargarProgreso() {
        val uidActual = uid ?: return
        cargaProgreso?.cancel()
        if (!conectado) {
            _progreso.value = ProgresoUiState.SinConexion
            return
        }
        _progreso.value = ProgresoUiState.Cargando
        cargaProgreso = viewModelScope.launch {
            val meta = when (val preferences = userPreferences?.leer(uidActual)) {
                null -> null
                is Resultado.Exito -> preferences.valor.weeklyGoalMinutes
                is Resultado.Fallo -> {
                    _progreso.value = if (preferences.error == ErrorApp.SinConexion) {
                        ProgresoUiState.SinConexion
                    } else {
                        ProgresoUiState.Error
                    }
                    return@launch
                }
            }
            when (val resultados = desafios.results(uidActual)) {
                is Resultado.Fallo -> _progreso.value = if (resultados.error == ErrorApp.SinConexion) {
                    ProgresoUiState.SinConexion
                } else {
                    ProgresoUiState.Error
                }
                is Resultado.Exito -> when (val avance = logros.actualizar(uidActual, resultados.valor)) {
                    is Resultado.Fallo -> _progreso.value = if (avance.error == ErrorApp.SinConexion) {
                        ProgresoUiState.SinConexion
                    } else {
                        ProgresoUiState.Error
                    }
                    is Resultado.Exito -> _progreso.value = ProgresoUiState.Datos(
                        metricas = ProgressCalculator.calcular(
                            resultados = resultados.valor,
                            ahora = clock.instant(),
                            zonaActual = clock.zone,
                            metaSemanalMinutos = meta,
                        ),
                        logros = avance.valor.sortedBy { it.definition.order },
                    )
                }
            }
        }
    }

    private fun cargarPreferencias() {
        val uidActual = uid ?: return
        val repository = userPreferences ?: return
        cargaPreferencias?.cancel()
        if (!conectado) {
            _preferencias.value = PreferenciasPerfilUiState.SinConexion
            return
        }
        _preferencias.value = PreferenciasPerfilUiState.Cargando
        cargaPreferencias = viewModelScope.launch {
            _preferencias.value = when (val result = repository.leer(uidActual)) {
                is Resultado.Exito -> PreferenciasPerfilUiState.Datos(result.valor)
                is Resultado.Fallo -> if (result.error == ErrorApp.SinConexion) {
                    PreferenciasPerfilUiState.SinConexion
                } else {
                    PreferenciasPerfilUiState.Error
                }
            }
        }
    }

    private fun cargar() {
        val uidActual = uid ?: return
        carga?.cancel()
        _edicionNombre.value = EdicionNombreUiState()
        cargarDesafiosHechos()
        cargarCanjes()
        cargarProximoVencimiento()
        cargarProgreso()
        cargarPreferencias()
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
