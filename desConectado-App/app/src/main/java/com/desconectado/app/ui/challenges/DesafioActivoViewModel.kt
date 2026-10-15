package com.desconectado.app.ui.challenges

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.desconectado.app.data.challenges.ActiveChallengeStore
import com.desconectado.app.data.notifications.DesconectadoNotifications
import com.desconectado.app.domain.SystemTimeSource
import com.desconectado.app.domain.TimeSource
import com.desconectado.app.domain.actualizarLogrosTrasResultado
import com.desconectado.app.domain.evaluarCumplimiento
import com.desconectado.app.domain.model.ActiveChallenge
import com.desconectado.app.domain.model.AchievementProgress
import com.desconectado.app.domain.model.ChallengeResult
import com.desconectado.app.domain.model.ChallengeRating
import com.desconectado.app.domain.model.Conectividad
import com.desconectado.app.domain.model.Desafio
import com.desconectado.app.domain.model.ErrorApp
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.repository.ChallengeRepository
import com.desconectado.app.domain.repository.AchievementRepository
import com.desconectado.app.domain.repository.UserPreferencesRepository
import com.desconectado.app.domain.repository.ConnectivityMonitor
import com.desconectado.app.domain.repository.UsageStatsRepository
import com.desconectado.app.domain.repository.PointsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.CancellationException
import com.desconectado.app.data.aErrorFirestore
import java.io.IOException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.Instant

sealed interface DesafioActivoUiState {
    data object SinDesafio : DesafioActivoUiState
    data object Cargando : DesafioActivoUiState
    data object Iniciando : DesafioActivoUiState
    data object Completando : DesafioActivoUiState
    data object SinPermiso : DesafioActivoUiState
    data class Activo(
        val desafio: ActiveChallenge,
        val usoSocialSeconds: Long = 0,
        val segundosRestantes: Long = desafio.durationSeconds.toLong(),
    ) : DesafioActivoUiState
    data class Terminado(
        val resultado: ChallengeResult,
        val ratingStars: Int? = null,
        val ratingSeleccionado: Int = 0,
        val ratingGuardando: Boolean = false,
        val ratingError: Boolean = false,
        val logrosDesbloqueados: List<AchievementProgress> = emptyList(),
    ) : DesafioActivoUiState
    data object SinConexion : DesafioActivoUiState
    data class Error(val causa: ErrorApp, val alFinalizar: Boolean = false) : DesafioActivoUiState
}

class DesafioActivoViewModel(
    private val uid: String,
    private val challenges: ChallengeRepository,
    private val usage: UsageStatsRepository,
    private val store: ActiveChallengeStore,
    private val points: PointsRepository,
    private val achievements: AchievementRepository,
    private val userPreferences: UserPreferencesRepository,
    private val notifications: DesconectadoNotifications,
    connectivity: ConnectivityMonitor,
    private val time: TimeSource = SystemTimeSource,
) : ViewModel() {
    private val _estado = MutableStateFlow<DesafioActivoUiState>(DesafioActivoUiState.Cargando)
    val estado: StateFlow<DesafioActivoUiState> = _estado.asStateFlow()
    private var conectado = true
    private var notificationsEnabled = false
    private var desafioPendiente: Desafio? = null
    private val operaciones = Mutex()
    private var cierrePendiente: ChallengeResult? = null

    init {
        viewModelScope.launch {
            userPreferences.observar(uid).collect {
                notificationsEnabled = it.notificationsEnabled
            }
        }
        viewModelScope.launch {
            connectivity.estado.collect { valor ->
                conectado = valor == Conectividad.CONECTADO
                if (!conectado && _estado.value is DesafioActivoUiState.Activo) {
                    _estado.value = DesafioActivoUiState.SinConexion
                } else if (conectado && _estado.value == DesafioActivoUiState.SinConexion) {
                    _estado.value = DesafioActivoUiState.Cargando
                    operaciones.withLock { restaurar() }
                }
            }
        }
        viewModelScope.launch { operaciones.withLock { restaurar() } }
        viewModelScope.launch {
            while (isActive) {
                delay(1000)
                operaciones.withLock { comprobarActual() }
            }
        }
    }

    fun iniciar(desafio: Desafio) {
        if (hayOperacionEnCurso() || cierrePendiente != null) return
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
        _estado.value = DesafioActivoUiState.Iniciando
        viewModelScope.launch {
            operaciones.withLock {
                when (val resultado = challenges.start(uid, desafio.id)) {
                    is Resultado.Exito -> {
                        store.write(resultado.valor)
                        _estado.value = estadoActivo(resultado.valor)
                    }
                    is Resultado.Fallo -> {
                        Log.e(TAG, "No se pudo iniciar el desafío: ${resultado.error}")
                        _estado.value = if (resultado.error == ErrorApp.SinConexion) DesafioActivoUiState.SinConexion else DesafioActivoUiState.Error(resultado.error)
                    }
                }
            }
        }
    }

    fun abrirAjustes() = usage.openUsageAccessSettings()

    fun volverCatalogo() {
        if (hayOperacionEnCurso() || cierrePendiente != null) return
        val terminal = _estado.value as? DesafioActivoUiState.Terminado
        if (terminal?.resultado?.status == ChallengeResult.Status.COMPLETED && terminal.ratingStars == null) return
        _estado.value = DesafioActivoUiState.SinDesafio
    }

    fun reintentarPermiso() {
        desafioPendiente?.let { iniciar(it) }
    }

    fun actualizar() {
        viewModelScope.launch { operaciones.withLock { comprobarActual() } }
    }

    private suspend fun comprobarActual() {
        val actual = (_estado.value as? DesafioActivoUiState.Activo)?.desafio ?: return
        if (!conectado) return
        if (!usage.hasUsageAccess()) {
            invalidar(actual, "usage_access_revoked")
            return
        }
        val ahora = time.now()
        when (val medicion = usage.socialUsageByPackageSeconds(actual.startedAt, ahora)) {
            is Resultado.Exito -> {
                val app = medicion.valor.entries.firstOrNull { it.value > 0 }?.key
                if (app != null) {
                    notifications.entroAUnaRed(nombreVisible(app), notificationsEnabled)
                    invalidar(actual, "social_app_used")
                } else {
                    val uso = medicion.valor.values.sum()
                    _estado.value = estadoActivo(actual, uso)
                    if (!ahora.isBefore(actual.startedAt.plusSeconds(actual.durationSeconds.toLong()))) {
                        finalizarActual(actual, ahora, uso)
                    }
                }
            }
            is Resultado.Fallo -> _estado.value = when (medicion.error) {
                ErrorApp.SinConexion -> DesafioActivoUiState.SinConexion
                ErrorApp.AccesoUsoDenegado -> DesafioActivoUiState.SinPermiso
                else -> DesafioActivoUiState.Error(medicion.error)
            }
        }
    }

    fun finalizar() {
        viewModelScope.launch {
            operaciones.withLock {
                val pendiente = cierrePendiente
                if (pendiente != null) cerrar(pendiente) else comprobarActual()
            }
        }
    }

    private suspend fun finalizarActual(actual: ActiveChallenge, ahora: Instant, uso: Long) {
        val resultado = evaluarCumplimiento(
            desafio = Desafio(
                actual.challengeId,
                actual.challengeTitle,
                "",
                actual.durationMinutes,
                com.desconectado.app.domain.model.Dificultad.FACIL,
                actual.points,
                0,
                durationSeconds = actual.durationSeconds,
                category = actual.category,
            ),
            measuredSocialSeconds = uso,
            offlineSeconds = actual.offlineSeconds,
            finishedAt = ahora,
            timeSource = time,
            startedAt = actual.startedAt,
        )
        cerrar(resultado)
    }

    fun cancelar() {
        viewModelScope.launch {
            operaciones.withLock {
                val actual = (_estado.value as? DesafioActivoUiState.Activo)?.desafio ?: return@withLock
                cerrar(resultadoInterrumpido(actual, ChallengeResult.Status.CANCELLED))
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

    private suspend fun invalidar(actual: ActiveChallenge, reason: String) {
        cerrar(resultadoInterrumpido(actual, ChallengeResult.Status.INVALIDATED).copy(invalidationReason = reason))
    }

    private fun resultadoInterrumpido(actual: ActiveChallenge, status: ChallengeResult.Status) = ChallengeResult(
        challengeRunId = runId(actual),
        challengeId = actual.challengeId,
        challengeTitle = actual.challengeTitle,
        durationMinutes = actual.durationMinutes,
        durationSeconds = actual.durationSeconds,
        startedAt = actual.startedAt,
        finishedAt = time.now(),
        status = status,
        measuredSocialSeconds = 0,
        offlineSeconds = actual.offlineSeconds,
        pointsAwarded = 0,
        category = actual.category,
    )

    private suspend fun cerrar(resultado: ChallengeResult) {
        cierrePendiente = resultado
        try {
            cerrarYNotificar(resultado)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Fallo de almacenamiento durante el cierre del desafío", e)
            _estado.value = DesafioActivoUiState.Error(ErrorApp.Desconocido, alFinalizar = true)
        } catch (e: RuntimeException) {
            Log.e(TAG, "Fallo inesperado durante el cierre del desafío", e)
            _estado.value = DesafioActivoUiState.Error(e.aErrorFirestore(), alFinalizar = true)
        }
        val fallo = _estado.value as? DesafioActivoUiState.Error
        if (fallo != null) _estado.value = fallo.copy(alFinalizar = true)
        if (_estado.value == DesafioActivoUiState.SinConexion) {
            _estado.value = DesafioActivoUiState.Error(ErrorApp.SinConexion, alFinalizar = true)
        }
    }

    private suspend fun cerrarYNotificar(resultado: ChallengeResult) {
        _estado.value = DesafioActivoUiState.Completando
        val historialPrevio = if (resultado.status == ChallengeResult.Status.COMPLETED) {
            when (val historial = challenges.results(uid)) {
                is Resultado.Exito -> historial.valor
                is Resultado.Fallo -> {
                    _estado.value = DesafioActivoUiState.Error(historial.error)
                    return
                }
            }
        } else {
            null
        }
        val logrosPrevios = historialPrevio?.let { historial ->
            (achievements.actualizar(uid, historial) as? Resultado.Exito)?.valor
                ?.filter { it.unlocked }
                ?.map { it.definition.id }
                ?.toSet()
        }
        when (val guardado = challenges.finish(uid, resultado.challengeRunId, resultado)) {
            is Resultado.Exito -> {
                val acreditacion = if (guardado.valor.status == ChallengeResult.Status.COMPLETED) {
                    points.acreditarConRacha(uid, guardado.valor, historialPrevio.orEmpty())
                } else {
                    Resultado.Exito(guardado.valor)
                }
                if (acreditacion is Resultado.Exito) {
                    val finalResult = acreditacion.valor
                    var logrosNuevos = emptyList<AchievementProgress>()
                    if (finalResult.status == ChallengeResult.Status.COMPLETED) {
                        when (val progreso = actualizarLogrosTrasResultado(uid, finalResult, challenges, achievements)) {
                            is Resultado.Exito -> {
                                if (logrosPrevios != null) {
                                    logrosNuevos = progreso.valor.filter {
                                        it.unlocked && it.definition.id !in logrosPrevios
                                    }
                                }
                            }
                            is Resultado.Fallo -> Log.w(TAG, "No se pudieron actualizar los logros tras completar el desafío")
                        }
                    }
                    store.clear()
                    notifications.logrosDesbloqueados(logrosNuevos.map { it.definition.name }, notificationsEnabled)
                    if (finalResult.status == ChallengeResult.Status.COMPLETED || finalResult.status == ChallengeResult.Status.FAILED) {
                        notifications.desafioTerminado(
                            finalResult.status == ChallengeResult.Status.COMPLETED,
                            finalResult.pointsAwarded,
                            notificationsEnabled,
                        )
                    }
                    mostrarResultadoTerminal(finalResult, logrosNuevos)
                    cierrePendiente = null
                } else if (acreditacion is Resultado.Fallo) {
                    _estado.value = DesafioActivoUiState.Error(acreditacion.error)
                }
            }
            is Resultado.Fallo -> _estado.value = if (guardado.error == ErrorApp.SinConexion) DesafioActivoUiState.SinConexion else DesafioActivoUiState.Error(guardado.error)
        }
    }

    private suspend fun mostrarResultadoTerminal(
        resultado: ChallengeResult,
        logrosDesbloqueados: List<AchievementProgress> = emptyList(),
    ) {
        if (resultado.status != ChallengeResult.Status.COMPLETED) {
            _estado.value = DesafioActivoUiState.Terminado(resultado, logrosDesbloqueados = logrosDesbloqueados)
            return
        }
        _estado.value = when (val rating = challenges.rating(uid, resultado.challengeRunId)) {
            is Resultado.Exito -> DesafioActivoUiState.Terminado(
                resultado = resultado,
                ratingStars = rating.valor?.stars,
                ratingSeleccionado = rating.valor?.stars ?: 0,
                logrosDesbloqueados = logrosDesbloqueados,
            )
            is Resultado.Fallo -> DesafioActivoUiState.Terminado(
                resultado,
                ratingError = true,
                logrosDesbloqueados = logrosDesbloqueados,
            )
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
                _estado.value = activo?.let { estadoActivo(it) } ?: DesafioActivoUiState.SinDesafio
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

    private fun estadoActivo(active: ActiveChallenge, uso: Long = 0): DesafioActivoUiState.Activo {
        val remainingMillis = Duration.between(time.now(), active.startedAt.plusSeconds(active.durationSeconds.toLong())).toMillis()
        val remainingSeconds = ((remainingMillis.coerceAtLeast(0) + 999) / 1000).coerceAtMost(active.durationSeconds.toLong())
        return DesafioActivoUiState.Activo(active, uso, remainingSeconds)
    }

    private fun runId(active: ActiveChallenge): String = "${active.challengeId}-${active.startedAt.epochSecond}"

    private fun hayOperacionEnCurso(): Boolean =
        _estado.value is DesafioActivoUiState.Activo ||
            _estado.value == DesafioActivoUiState.Iniciando ||
            _estado.value == DesafioActivoUiState.Completando

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
