package com.desconectado.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desconectado.app.domain.ErrorCampo
import com.desconectado.app.domain.model.Conectividad
import com.desconectado.app.domain.model.ErrorApp
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.model.errorOrNull
import com.desconectado.app.domain.normalizarCorreo
import com.desconectado.app.domain.repository.AuthRepository
import com.desconectado.app.domain.repository.ConnectivityMonitor
import com.desconectado.app.domain.validarCorreo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IngresoUiState(
    val email: String = "",
    val password: String = "",
    val errorCorreo: ErrorCampo? = null,
    val errorPassword: ErrorCampo? = null,
    val enviando: Boolean = false,
    val sinConexion: Boolean = false,
    /** Error de la última petición; la pantalla lo traduce a un mensaje. */
    val errorEnvio: ErrorApp? = null,
    /** Diálogo para vincular Google a una cuenta de contraseña existente; `null` si no hay ninguno. */
    val vinculacion: VinculacionPendiente? = null,
)

class IngresoViewModel(
    private val auth: AuthRepository,
    conectividad: ConnectivityMonitor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(IngresoUiState())
    val uiState: StateFlow<IngresoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            conectividad.estado.collect { estado ->
                _uiState.update { it.copy(sinConexion = estado == Conectividad.SIN_CONEXION) }
            }
        }
    }

    fun onEmailChange(valor: String) = _uiState.update {
        it.copy(email = valor, errorCorreo = null, errorEnvio = null)
    }

    fun onPasswordChange(valor: String) = _uiState.update {
        it.copy(password = valor, errorPassword = null, errorEnvio = null)
    }

    fun ingresar() {
        val actual = _uiState.value
        if (actual.enviando) return // doble toque

        // Solo se valida que haya algo escrito y que el correo tenga forma de correo; si los datos
        // no coinciden con una cuenta, el mensaje es siempre el mismo (FR-004).
        val errorCorreo = validarCorreo(actual.email)
        val errorPassword = if (actual.password.isEmpty()) ErrorCampo.VACIO else null
        if (errorCorreo != null || errorPassword != null) {
            _uiState.update { it.copy(errorCorreo = errorCorreo, errorPassword = errorPassword, errorEnvio = null) }
            return
        }
        if (actual.sinConexion) {
            _uiState.update { it.copy(errorEnvio = ErrorApp.SinConexion) }
            return
        }

        _uiState.update { it.copy(enviando = true, errorEnvio = null) }
        viewModelScope.launch {
            val resultado = auth.ingresar(normalizarCorreo(actual.email), actual.password)
            _uiState.update { it.copy(enviando = false, errorEnvio = resultado.errorOrNull()) }
        }
    }

    /** Recibe el token de Google ya obtenido por la interfaz, o el motivo por el que no se obtuvo. */
    fun continuarConGoogle(token: Resultado<String>) {
        val actual = _uiState.value
        if (actual.enviando) return
        when (token) {
            is Resultado.Fallo -> {
                // Cancelar la pantalla de Google vuelve al formulario sin mensaje de error.
                if (token.error != ErrorApp.Cancelado) _uiState.update { it.copy(errorEnvio = token.error) }
            }
            is Resultado.Exito -> {
                if (actual.sinConexion) {
                    _uiState.update { it.copy(errorEnvio = ErrorApp.SinConexion) }
                    return
                }
                _uiState.update { it.copy(enviando = true, errorEnvio = null) }
                viewModelScope.launch {
                    val resultado = auth.ingresarConGoogle(token.valor)
                    _uiState.update {
                        if (resultado.errorOrNull() == ErrorApp.CuentaExistenteConOtroProveedor) {
                            it.copy(
                                enviando = false,
                                vinculacion = VinculacionPendiente(token.valor, email = normalizarCorreo(it.email)),
                            )
                        } else {
                            it.copy(enviando = false, errorEnvio = resultado.errorOrNull())
                        }
                    }
                }
            }
        }
    }

    fun onVinculacionEmailChange(valor: String) = _uiState.update {
        it.copy(vinculacion = it.vinculacion?.copy(email = valor, error = null))
    }

    fun onVinculacionPasswordChange(valor: String) = _uiState.update {
        it.copy(vinculacion = it.vinculacion?.copy(password = valor, error = null))
    }

    fun cancelarVinculacion() = _uiState.update { it.copy(vinculacion = null) }

    fun confirmarVinculacion() {
        val actual = _uiState.value
        val vinculacion = actual.vinculacion ?: return
        if (vinculacion.enviando) return
        if (actual.sinConexion) {
            _uiState.update { it.copy(vinculacion = vinculacion.copy(error = ErrorApp.SinConexion)) }
            return
        }
        _uiState.update { it.copy(vinculacion = vinculacion.copy(enviando = true, error = null)) }
        viewModelScope.launch {
            val resultado = completarVinculacion(auth, vinculacion)
            _uiState.update {
                if (resultado is Resultado.Exito) {
                    it.copy(vinculacion = null)
                } else {
                    it.copy(vinculacion = vinculacion.copy(enviando = false, error = resultado.errorOrNull()))
                }
            }
        }
    }
}
