package com.desconectado.app.ui.rewards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desconectado.app.domain.model.ErrorApp
import com.desconectado.app.domain.model.Recompensa
import com.desconectado.app.domain.model.RedeemedReward
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.repository.CatalogRepository
import com.desconectado.app.domain.repository.ConnectivityMonitor
import com.desconectado.app.domain.repository.PointsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface RecompensasCanjeUiState {
    data object Cargando : RecompensasCanjeUiState
    data class Lista(val recompensas: List<Recompensa>, val canjeadas: List<RedeemedReward>) : RecompensasCanjeUiState
    data object Error : RecompensasCanjeUiState
    data object SinConexion : RecompensasCanjeUiState
    data object SaldoInsuficiente : RecompensasCanjeUiState
    data class CanjeExitoso(val recompensa: RedeemedReward) : RecompensasCanjeUiState
}

enum class CanjeFeedback { SaldoInsuficiente, Exitoso, SinConexion, FirestoreNoAutorizado, Error }

class RecompensasCanjeViewModel(
    private val uid: String,
    private val catalog: CatalogRepository,
    private val points: PointsRepository,
    private val connectivity: ConnectivityMonitor,
) : ViewModel() {
    private val _estado = MutableStateFlow<RecompensasCanjeUiState>(RecompensasCanjeUiState.Cargando)
    val estado: StateFlow<RecompensasCanjeUiState> = _estado.asStateFlow()
    private val feedbackChannel = Channel<CanjeFeedback>(Channel.BUFFERED)
    val feedback = feedbackChannel.receiveAsFlow()
    private var cargando = false

    init { cargar() }

    fun recargar() = cargar()

    fun canjear(recompensa: Recompensa, redemptionId: String) {
        if (cargando) return
        viewModelScope.launch {
            cargando = true
            when (val saldo = points.saldo(uid)) {
                is Resultado.Exito -> if (saldo.valor < recompensa.costPoints) {
                    feedbackChannel.trySend(CanjeFeedback.SaldoInsuficiente)
                } else when (val result = points.redeem(uid, recompensa, redemptionId)) {
                    is Resultado.Exito -> {
                        _estado.value = RecompensasCanjeUiState.CanjeExitoso(result.valor)
                        feedbackChannel.trySend(CanjeFeedback.Exitoso)
                    }
                    is Resultado.Fallo -> feedbackChannel.trySend(
                        when (result.error) {
                            ErrorApp.SinConexion -> CanjeFeedback.SinConexion
                            ErrorApp.SaldoInsuficiente -> CanjeFeedback.SaldoInsuficiente
                            ErrorApp.FirestoreNoAutorizado -> CanjeFeedback.FirestoreNoAutorizado
                            else -> CanjeFeedback.Error
                        },
                    )
                }
                is Resultado.Fallo -> feedbackChannel.trySend(when (saldo.error) {
                    ErrorApp.SinConexion -> CanjeFeedback.SinConexion
                    ErrorApp.FirestoreNoAutorizado -> CanjeFeedback.FirestoreNoAutorizado
                    else -> CanjeFeedback.Error
                })
            }
            cargando = false
        }
    }

    private fun cargar() {
        viewModelScope.launch {
            if (connectivity.estadoValue() == com.desconectado.app.domain.model.Conectividad.SIN_CONEXION) {
                _estado.value = RecompensasCanjeUiState.SinConexion
                return@launch
            }
            _estado.value = RecompensasCanjeUiState.Cargando
            val catalogo = catalog.recompensas()
            val canjeadas = points.recompensasCanjeadas(uid)
            _estado.value = when {
                catalogo is Resultado.Exito && canjeadas is Resultado.Exito ->
                    RecompensasCanjeUiState.Lista(catalogo.valor.sortedBy { it.order }, canjeadas.valor)
                catalogo is Resultado.Fallo && catalogo.error == ErrorApp.SinConexion -> RecompensasCanjeUiState.SinConexion
                canjeadas is Resultado.Fallo && canjeadas.error == ErrorApp.SinConexion -> RecompensasCanjeUiState.SinConexion
                else -> RecompensasCanjeUiState.Error
            }
        }
    }

    private suspend fun ConnectivityMonitor.estadoValue() = estado.first()
}
