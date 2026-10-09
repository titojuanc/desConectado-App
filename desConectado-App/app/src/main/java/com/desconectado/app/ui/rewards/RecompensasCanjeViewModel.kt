package com.desconectado.app.ui.rewards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desconectado.app.domain.model.ErrorApp
import com.desconectado.app.domain.model.CosmeticOwnership
import com.desconectado.app.domain.model.CosmeticPreferences
import com.desconectado.app.domain.model.Recompensa
import com.desconectado.app.domain.model.RedeemedReward
import com.desconectado.app.domain.model.PendingRedemption
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.model.TipoRecompensa
import com.desconectado.app.domain.codigoCupon
import com.desconectado.app.domain.cosmeticosPoseidos
import com.desconectado.app.domain.esCosmetico
import com.desconectado.app.domain.repository.CatalogRepository
import com.desconectado.app.domain.repository.ConnectivityMonitor
import com.desconectado.app.domain.repository.PointsRepository
import com.desconectado.app.domain.repository.CosmeticPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface RecompensasCanjeUiState {
    data object Cargando : RecompensasCanjeUiState
    data class Lista(
        val recompensas: List<Recompensa>,
        val canjeadas: List<RedeemedReward>,
        val pendiente: PendingRedemption? = null,
    ) : RecompensasCanjeUiState
    data object Error : RecompensasCanjeUiState
    data object SinConexion : RecompensasCanjeUiState
    data object SaldoInsuficiente : RecompensasCanjeUiState
    data class CanjeExitoso(val recompensa: RedeemedReward) : RecompensasCanjeUiState
}

enum class CanjeFeedback { SaldoInsuficiente, RecompensaNoDisponible, Exitoso, SinConexion, FirestoreNoAutorizado, Error }

class RecompensasCanjeViewModel(
    private val uid: String,
    private val catalog: CatalogRepository,
    private val points: PointsRepository,
    private val cosmeticPreferences: CosmeticPreferencesRepository,
    private val connectivity: ConnectivityMonitor,
) : ViewModel() {
    private val _estado = MutableStateFlow<RecompensasCanjeUiState>(RecompensasCanjeUiState.Cargando)
    val estado: StateFlow<RecompensasCanjeUiState> = _estado.asStateFlow()
    private val feedbackChannel = Channel<CanjeFeedback>(Channel.BUFFERED)
    val feedback = feedbackChannel.receiveAsFlow()
    private val _propiedad = MutableStateFlow<List<CosmeticOwnership>>(emptyList())
    val propiedad: StateFlow<List<CosmeticOwnership>> = _propiedad.asStateFlow()
    private val _preferencias = MutableStateFlow(CosmeticPreferences())
    val preferencias: StateFlow<CosmeticPreferences> = _preferencias.asStateFlow()
    private var cargando = false

    init { cargar() }

    fun recargar() = cargar()

    fun canjear(recompensa: Recompensa, redemptionId: String) {
        if (cargando) return
        viewModelScope.launch {
            cargando = true
            try {
                when (val saldo = points.saldo(uid)) {
                    is Resultado.Exito -> if (saldo.valor < recompensa.costPoints) {
                        feedbackChannel.trySend(CanjeFeedback.SaldoInsuficiente)
                    } else {
                        val lista = _estado.value as? RecompensasCanjeUiState.Lista
                        if (lista != null) {
                            _estado.value = lista.copy(pendiente = PendingRedemption(
                                redemptionId = redemptionId,
                                rewardId = recompensa.id,
                                name = recompensa.name,
                                costPoints = recompensa.costPoints,
                                code = if (recompensa.kind == TipoRecompensa.CUPON) codigoCupon(redemptionId) else null,
                                pointsDebited = 0,
                                lotDebits = emptyMap(),
                                createdAt = java.time.Instant.now(),
                            ))
                        }
                        when (val result = points.redeem(uid, recompensa, redemptionId)) {
                            is Resultado.Exito -> {
                                _propiedad.value = (_propiedad.value + cosmeticosPoseidos(listOf(result.valor)))
                                    .distinctBy { it.rewardId }
                                _estado.value = RecompensasCanjeUiState.CanjeExitoso(result.valor)
                                feedbackChannel.trySend(CanjeFeedback.Exitoso)
                            }
                            is Resultado.Fallo -> {
                                val pendingResult = points.pendingRedemption(uid)
                                val currentList = _estado.value as? RecompensasCanjeUiState.Lista
                                if (currentList != null && pendingResult is Resultado.Exito) {
                                    _estado.value = currentList.copy(pendiente = pendingResult.valor)
                                }
                                feedbackChannel.trySend(when (result.error) {
                                    ErrorApp.SinConexion -> CanjeFeedback.SinConexion
                                    ErrorApp.SaldoInsuficiente -> CanjeFeedback.SaldoInsuficiente
                                    ErrorApp.RecompensaNoDisponible -> CanjeFeedback.RecompensaNoDisponible
                                    ErrorApp.FirestoreNoAutorizado -> CanjeFeedback.FirestoreNoAutorizado
                                    else -> CanjeFeedback.Error
                                })
                            }
                        }
                    }
                    is Resultado.Fallo -> feedbackChannel.trySend(when (saldo.error) {
                        ErrorApp.SinConexion -> CanjeFeedback.SinConexion
                        ErrorApp.FirestoreNoAutorizado -> CanjeFeedback.FirestoreNoAutorizado
                        else -> CanjeFeedback.Error
                    })
                }
            } finally {
                cargando = false
            }
        }
    }

    fun activarCosmetico(rewardId: String) {
        val cosmetico = _propiedad.value.firstOrNull { it.rewardId == rewardId } ?: return
        seleccionarCosmetico(cosmetico.kind, rewardId)
    }

    fun quitarCosmetico(kind: TipoRecompensa) {
        seleccionarCosmetico(kind, null)
    }

    private fun seleccionarCosmetico(kind: TipoRecompensa, rewardId: String?) {
        if (!kind.esCosmetico()) return
        if (rewardId != null && _propiedad.value.none { it.rewardId == rewardId && it.kind == kind }) return
        viewModelScope.launch {
            when (val resultado = cosmeticPreferences.seleccionar(uid, kind, rewardId)) {
                is Resultado.Exito -> _preferencias.value = resultado.valor
                is Resultado.Fallo -> feedbackChannel.trySend(when (resultado.error) {
                    ErrorApp.SinConexion -> CanjeFeedback.SinConexion
                    ErrorApp.FirestoreNoAutorizado -> CanjeFeedback.FirestoreNoAutorizado
                    else -> CanjeFeedback.Error
                })
            }
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
            val pendiente = points.pendingRedemption(uid)
            val preferencias = cosmeticPreferences.leer(uid)
            if (preferencias is Resultado.Exito) _preferencias.value = preferencias.valor
            _estado.value = when {
                catalogo is Resultado.Exito && canjeadas is Resultado.Exito && pendiente is Resultado.Exito -> {
                    _propiedad.value = cosmeticosPoseidos(canjeadas.valor)
                    val lista = RecompensasCanjeUiState.Lista(
                        catalogo.valor.sortedBy { it.order },
                        canjeadas.valor,
                        pendiente.valor,
                    )
                    _estado.value = lista
                    if (pendiente.valor != null) {
                        when (val reanudado = points.resumePendingRedemption(uid)) {
                            is Resultado.Exito -> reanudado.valor?.let {
                                _propiedad.value = (_propiedad.value + cosmeticosPoseidos(listOf(it)))
                                    .distinctBy { cosmetico -> cosmetico.rewardId }
                                feedbackChannel.trySend(CanjeFeedback.Exitoso)
                                RecompensasCanjeUiState.CanjeExitoso(it)
                            } ?: lista.copy(pendiente = null)
                            is Resultado.Fallo -> {
                                feedbackChannel.trySend(if (reanudado.error == ErrorApp.SinConexion) {
                                    CanjeFeedback.SinConexion
                                } else {
                                    CanjeFeedback.Error
                                })
                                when (val refreshed = points.pendingRedemption(uid)) {
                                    is Resultado.Exito -> lista.copy(pendiente = refreshed.valor)
                                    is Resultado.Fallo -> lista
                                }
                            }
                        }
                    } else {
                        lista
                    }
                }
                catalogo is Resultado.Fallo && catalogo.error == ErrorApp.SinConexion -> RecompensasCanjeUiState.SinConexion
                canjeadas is Resultado.Fallo && canjeadas.error == ErrorApp.SinConexion -> RecompensasCanjeUiState.SinConexion
                pendiente is Resultado.Fallo && pendiente.error == ErrorApp.SinConexion -> RecompensasCanjeUiState.SinConexion
                else -> RecompensasCanjeUiState.Error
            }
        }
    }

    private suspend fun ConnectivityMonitor.estadoValue() = estado.first()
}
