package com.desconectado.app.ui.challenges

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.desconectado.app.data.challenges.ActiveChallengeStore
import com.desconectado.app.data.notifications.DesconectadoNotifications
import com.desconectado.app.domain.SystemTimeSource
import com.desconectado.app.domain.TimeSource
import com.desconectado.app.domain.evaluarCumplimiento
import com.desconectado.app.domain.model.ActiveChallenge
import com.desconectado.app.domain.model.ChallengeResult
import com.desconectado.app.domain.model.ChallengeRating
import com.desconectado.app.domain.model.Conectividad
import com.desconectado.app.domain.model.Desafio
import com.desconectado.app.domain.model.ErrorApp
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.repository.ChallengeRepository
import com.desconectado.app.domain.repository.ConnectivityMonitor
import com.desconectado.app.domain.repository.UsageStatsRepository
import com.desconectado.app.domain.repository.PointsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

sealed interface DesafioActivoUiState {
    data object SinDesafio : DesafioActivoUiState
    data object Cargando : DesafioActivoUiState
    data object SinPermiso : DesafioActivoUiState
    data class Activo(val desafio: ActiveChallenge, val usoSocialSeconds: Long = 0) : DesafioActivoUiState
    data class Terminado(
        val resultado: ChallengeResult,
        val ratingStars: Int? = null,
        val ratingSeleccionado: Int = 0,
        val ratingGuardando: Boolean = false,
        val ratingError: Boolean = false,
    ) : DesafioActivoUiState
    data object SinConexion : DesafioActivoUiState
    data class Error(val causa: ErrorApp) : DesafioActivoUiState
}

class DesafioActivoViewModel(
    private val uid: String,
    private val challenges: ChallengeRepository,
    private val usage: UsageStatsRepository,
    private val store: ActiveChallengeStore,
    private val points: PointsRepository,
    private val notifications: DesconectadoNotifications,
    connectivity: ConnectivityMonitor,
    private val time: TimeSource = SystemTimeSource,
) : ViewModel() {
    private val _estado = MutableStateFlow<DesafioActivoUiState>(DesafioActivoUiState.Cargando)
    val estado: StateFlow<DesafioActivoUiState> = _estado.asStateFlow()
    private var conectado = true
    private var desafioPendiente: Desafio? = null

    init {
        viewModelScope.launch {
            connectivity.estado.collect { valor ->
                conectado = valor == Conectividad.CONECTADO
                if (!conectado && _estado.value is DesafioActivoUiState.Activo) {
                    _estado.value = DesafioActivoUiState.SinConexion
                } else if (conectado && _estado.value == DesafioActivoUiState.Cargando) {
                    restaurar()
                }
            }
        }
        viewModelScope.launch { restaurar() }
    }

    fun iniciar(desafio: Desafio) {
        desafioPendiente = desafio
        if (!conectado) {
            _estado.value = DesafioActivoUiState.SinConexion
            return
        }
        if (!usage.hasUsageAccess()) {
            _estado.value = DesafioActivoUiState.SinPermiso
            usage.openUsageAccessSettings()
            return
        }
        viewModelScope.launch {
            _estado.value = DesafioActivoUiState.Cargando
            when (val resultado = challenges.start(uid, desafio.id)) {
                is Resultado.Exito -> {
                    store.write(resultado.valor)
                    _estado.value = DesafioActivoUiState.Activo(resultado.valor)
                }
                is Resultado.Fallo -> {
                    Log.e(TAG, "No se pudo iniciar el desafío: ${resultado.error}")
                    _estado.value = if (resultado.error == ErrorApp.SinConexion) DesafioActivoUiState.SinConexion else DesafioActivoUiState.Error(resultado.error)
                }
            }
        }
    }

    fun abrirAjustes() = usage.openUsageAccessSettings()

    fun reintentarPermiso() {
        desafioPendiente?.let { iniciar(it) }
    }

    fun actualizar() {
        val actual = (_estado.value as? DesafioActivoUiState.Activo)?.desafio ?: return
        if (!usage.hasUsageAccess()) {
            invalidar(actual, "usage_access_revoked")
            return
        }
        viewModelScope.launch {
            when (val medicion = usage.socialUsageByPackageSeconds(actual.startedAt, time.now())) {
                is Resultado.Exito -> {
                    val app = medicion.valor.entries.firstOrNull { it.value > 0 }?.key
                    if (app != null) {
                        notifications.entroAUnaRed(nombreVisible(app))
                        invalidar(actual, "social_app_used")
                    } else {
                        _estado.value = DesafioActivoUiState.Activo(actual, medicion.valor.values.sum())
                    }
                }
                is Resultado.Fallo -> _estado.value = when (medicion.error) {
                    ErrorApp.SinConexion -> DesafioActivoUiState.SinConexion
                    ErrorApp.AccesoUsoDenegado -> DesafioActivoUiState.SinPermiso
                    else -> DesafioActivoUiState.Error(medicion.error)
                }
            }
        }
    }

    fun finalizar() {
        val actual = (_estado.value as? DesafioActivoUiState.Activo)?.desafio ?: return
        viewModelScope.launch {
            val ahora = time.now()
            val medicion = usage.socialUsageByPackageSeconds(actual.startedAt, ahora)
            val uso = (medicion as? Resultado.Exito)?.valor?.values?.sum() ?: return@launch
            val resultado = evaluarCumplimiento(
                desafio = Desafio(actual.challengeId, actual.challengeTitle, "", actual.durationMinutes, com.desconectado.app.domain.model.Dificultad.FACIL, actual.points, 0, actual.durationSeconds),
                measuredSocialSeconds = uso,
                offlineSeconds = actual.offlineSeconds,
                finishedAt = ahora,
                timeSource = time,
                startedAt = actual.startedAt,
            )
            cerrar(resultado)
        }
    }

    fun cancelar() {
        val actual = (_estado.value as? DesafioActivoUiState.Activo)?.desafio ?: return
        viewModelScope.launch {
            val resultado = challenges.cancel(uid, runId(actual))
            if (resultado is Resultado.Exito) {
                store.clear()
                _estado.value = DesafioActivoUiState.Terminado(resultado.valor)
            }
        }
    }

    fun seleccionarCalificacion(stars: Int) {
        val terminal = _estado.value as? DesafioActivoUiState.Terminado ?: return
        if (terminal.resultado.status != ChallengeResult.Status.COMPLETED || terminal.ratingStars != null) return
        if (stars !in ChallengeRating.MIN_STARS..ChallengeRating.MAX_STARS) return
        _estado.value = terminal.copy(ratingSeleccionado = stars, ratingError = false)
    }

    fun calificar() {
        val terminal = _estado.value as? DesafioActivoUiState.Terminado ?: return
        if (terminal.resultado.status != ChallengeResult.Status.COMPLETED || terminal.ratingStars != null) return
        if (terminal.ratingSeleccionado !in ChallengeRating.MIN_STARS..ChallengeRating.MAX_STARS || terminal.ratingGuardando) return
        val rating = ChallengeRating(terminal.resultado.challengeRunId, terminal.ratingSeleccionado, time.now())
        _estado.value = terminal.copy(ratingGuardando = true, ratingError = false)
        viewModelScope.launch {
            when (challenges.rate(uid, rating)) {
                is Resultado.Exito -> _estado.value = terminal.copy(
                    ratingStars = rating.stars,
                    ratingSeleccionado = rating.stars,
                    ratingGuardando = false,
                    ratingError = false,
                )
                is Resultado.Fallo -> _estado.value = terminal.copy(ratingGuardando = false, ratingError = true)
            }
        }
    }

    private fun invalidar(actual: ActiveChallenge, reason: String) {
        viewModelScope.launch {
            val resultado = challenges.invalidate(uid, runId(actual), reason)
            if (resultado is Resultado.Exito) {
                store.clear()
                _estado.value = DesafioActivoUiState.Terminado(resultado.valor)
            }
        }
    }

    private suspend fun cerrar(resultado: ChallengeResult) {
        when (val guardado = challenges.finish(uid, resultado.challengeRunId, resultado)) {
            is Resultado.Exito -> {
                val finalResult = guardado.valor
                val acreditacion = if (finalResult.status == ChallengeResult.Status.COMPLETED) {
                    points.acreditar(uid, finalResult)
                } else {
                    Resultado.Exito(Unit)
                }
                if (acreditacion is Resultado.Exito) {
                    store.clear()
                    notifications.desafioTerminado(finalResult.status == ChallengeResult.Status.COMPLETED, finalResult.pointsAwarded)
                    mostrarResultadoTerminal(finalResult)
                } else if (acreditacion is Resultado.Fallo) {
                    _estado.value = DesafioActivoUiState.Error(acreditacion.error)
                }
            }
            is Resultado.Fallo -> _estado.value = if (guardado.error == ErrorApp.SinConexion) DesafioActivoUiState.SinConexion else DesafioActivoUiState.Error(guardado.error)
        }
    }

    private suspend fun mostrarResultadoTerminal(resultado: ChallengeResult) {
        if (resultado.status != ChallengeResult.Status.COMPLETED) {
            _estado.value = DesafioActivoUiState.Terminado(resultado)
            return
        }
        _estado.value = when (val rating = challenges.rating(uid, resultado.challengeRunId)) {
            is Resultado.Exito -> DesafioActivoUiState.Terminado(
                resultado = resultado,
                ratingStars = rating.valor?.stars,
                ratingSeleccionado = rating.valor?.stars ?: 0,
            )
            is Resultado.Fallo -> DesafioActivoUiState.Terminado(resultado, ratingError = true)
        }
    }

    private suspend fun restaurar() {
        if (_estado.value != DesafioActivoUiState.Cargando) return
        if (!conectado) {
            _estado.value = DesafioActivoUiState.SinConexion
            return
        }
        when (val resultado = challenges.active(uid)) {
            is Resultado.Exito -> {
                val activo = resultado.valor ?: store.read()
                _estado.value = activo?.let { DesafioActivoUiState.Activo(it) } ?: DesafioActivoUiState.SinDesafio
            }
            is Resultado.Fallo -> {
                Log.e(TAG, "No se pudo restaurar el desafío: ${resultado.error}")
                _estado.value = when (resultado.error) {
                ErrorApp.SinConexion -> DesafioActivoUiState.SinConexion
                ErrorApp.AccesoUsoDenegado -> DesafioActivoUiState.SinPermiso
                else -> DesafioActivoUiState.Error(resultado.error)
                }
            }
        }
    }

    private fun runId(active: ActiveChallenge): String = "${active.challengeId}-${active.startedAt.epochSecond}"

    private fun nombreVisible(packageName: String): String = when (packageName) {
        "com.instagram.android" -> "Instagram"
        "com.zhiliaoapp.musically" -> "TikTok"
        "com.facebook.katana" -> "Facebook"
        "com.twitter.android" -> "X"
        "com.snapchat.android" -> "Snapchat"
        "com.google.android.youtube" -> "YouTube"
        else -> "una red social"
    }

    private companion object {
        const val TAG = "DesafioActivo"
    }
}
