package com.desconectado.app.ui.points

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desconectado.app.domain.model.Conectividad
import com.desconectado.app.domain.model.ErrorApp
import com.desconectado.app.domain.model.EstadoSesion
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.repository.AuthRepository
import com.desconectado.app.domain.repository.ConnectivityMonitor
import com.desconectado.app.domain.repository.ProfileRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Saldo que muestra el indicador de puntos de las tres pestañas (FR-028, FR-033). */
sealed interface SaldoUiState {
    /** Todavía no se sabe: el indicador muestra un guion. */
    data object Cargando : SaldoUiState

    data class Disponible(val puntos: Int) : SaldoUiState

    /** Sin conexión o con un fallo de carga: el indicador muestra un guion, nunca un 0 inventado. */
    data object NoDisponible : SaldoUiState
}

/**
 * Saldo de puntos de la sesión actual, leído del perfil (`pointsBalance`). Lo comparten las tres
 * pestañas; se recarga al cambiar de pestaña con [recargar]. Solo lectura (FR-032).
 *
 * Si la primera lectura falla por algo que no es la conexión (por ejemplo, en el primer ingreso con
 * Google, cuando la sesión ya empezó pero el perfil todavía se está creando), reintenta sola hasta
 * [REINTENTOS_AUTOMATICOS] veces cada [reintentoMs] milisegundos, para no dejar el guion hasta que
 * la persona cambie de pestaña.
 */
class SaldoViewModel(
    private val auth: AuthRepository,
    private val perfiles: ProfileRepository,
    conectividad: ConnectivityMonitor,
    private val reintentoMs: Long = 2_000,
) : ViewModel() {

    private val _estado = MutableStateFlow<SaldoUiState>(SaldoUiState.Cargando)
    val estado: StateFlow<SaldoUiState> = _estado.asStateFlow()

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
                                recargar()
                            }
                            _estado.value !is SaldoUiState.Disponible -> recargar()
                        }
                        // Al cerrar sesión no queda ningún saldo de la persona anterior.
                        EstadoSesion.SinSesion -> {
                            carga?.cancel()
                            uid = null
                            _estado.value = SaldoUiState.Cargando
                        }
                        EstadoSesion.Cargando -> Unit
                    }
                }
        }
    }

    /** Vuelve a pedir el saldo. Mientras recarga conserva el que ya se mostraba, sin parpadeos. */
    fun recargar() {
        val uidActual = uid ?: return
        carga?.cancel()
        if (!conectado) {
            _estado.value = SaldoUiState.NoDisponible
            return
        }
        if (_estado.value !is SaldoUiState.Disponible) _estado.value = SaldoUiState.Cargando
        carga = viewModelScope.launch {
            var reintentos = 0
            while (true) {
                when (val resultado = perfiles.perfil(uidActual)) {
                    is Resultado.Exito -> {
                        _estado.value = SaldoUiState.Disponible(resultado.valor.puntos)
                        return@launch
                    }
                    is Resultado.Fallo -> {
                        _estado.value = SaldoUiState.NoDisponible
                        if (resultado.error == ErrorApp.SinConexion || reintentos >= REINTENTOS_AUTOMATICOS) return@launch
                        reintentos++
                        delay(reintentoMs)
                    }
                }
            }
        }
    }

    private companion object {
        const val REINTENTOS_AUTOMATICOS = 2
    }
}
