package com.desconectado.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desconectado.app.domain.ErrorCampo
import com.desconectado.app.domain.model.Conectividad
import com.desconectado.app.domain.model.ErrorApp
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.normalizarCorreo
import com.desconectado.app.domain.repository.AuthRepository
import com.desconectado.app.domain.repository.ConnectivityMonitor
import com.desconectado.app.domain.validarCorreo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RestablecerPasswordUiState(
    val email: String = "",
    val errorCorreo: ErrorCampo? = null,
    val enviando: Boolean = false,
    val sinConexion: Boolean = false,
    /** El pedido se envió: se muestra siempre el mismo mensaje, exista o no la cuenta (FR-026). */
    val confirmado: Boolean = false,
    val errorEnvio: ErrorApp? = null,
)

class RestablecerPasswordViewModel(
    private val auth: AuthRepository,
    conectividad: ConnectivityMonitor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RestablecerPasswordUiState())
    val uiState: StateFlow<RestablecerPasswordUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            conectividad.estado.collect { estado ->
                _uiState.update { it.copy(sinConexion = estado == Conectividad.SIN_CONEXION) }
            }
        }
    }

    fun onEmailChange(valor: String) = _uiState.update {
        it.copy(email = valor, errorCorreo = null, errorEnvio = null, confirmado = false)
    }

    fun enviar() {
        val actual = _uiState.value
        if (actual.enviando) return // un segundo envío mientras el primero está en curso

        val errorCorreo = validarCorreo(actual.email)
        if (errorCorreo != null) {
            _uiState.update { it.copy(errorCorreo = errorCorreo, errorEnvio = null, confirmado = false) }
            return
        }
        if (actual.sinConexion) {
            _uiState.update { it.copy(errorEnvio = ErrorApp.SinConexion, confirmado = false) }
            return
        }

        _uiState.update { it.copy(enviando = true, errorEnvio = null, confirmado = false) }
        viewModelScope.launch {
            val resultado = auth.restablecerPassword(normalizarCorreo(actual.email))
            _uiState.update {
                when (resultado) {
                    is Resultado.Exito -> it.copy(enviando = false, confirmado = true)
                    is Resultado.Fallo -> it.copy(enviando = false, errorEnvio = resultado.error)
                }
            }
        }
    }
}
