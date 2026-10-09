package com.desconectado.app.ui.rewards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.desconectado.app.R
import com.desconectado.app.domain.model.Recompensa
import com.desconectado.app.domain.model.PendingRedemption
import com.desconectado.app.domain.model.CosmeticOwnership
import com.desconectado.app.domain.model.CosmeticPreferences
import com.desconectado.app.domain.model.TipoRecompensa
import com.desconectado.app.domain.esCosmetico
import com.desconectado.app.ui.components.PantallaCargando
import com.desconectado.app.ui.components.PantallaError
import com.desconectado.app.ui.components.PantallaSinConexion
import kotlinx.coroutines.flow.Flow

/** Catálogo de recompensas: solo se consulta, no hay acción de canje (FR-020). */
@Composable
fun RecompensasScreen(
    estado: RecompensasUiState,
    onReintentar: () -> Unit,
    onCanjear: ((Recompensa) -> Unit)? = null,
    feedbackEvents: Flow<CanjeFeedback>? = null,
    canjePendiente: PendingRedemption? = null,
    propiedad: List<CosmeticOwnership> = emptyList(),
    preferencias: CosmeticPreferences = CosmeticPreferences(),
    onAplicarCosmetico: ((String) -> Unit)? = null,
    onQuitarCosmetico: ((TipoRecompensa) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val snackbar = remember { SnackbarHostState() }
    val mensajeInsuficiente = stringResource(R.string.canje_saldo_insuficiente)
    val mensajeSinPremios = stringResource(R.string.canje_caja_sin_premios)
    val mensajeExito = stringResource(R.string.canje_exitoso)
    val mensajeSinConexion = stringResource(R.string.desafio_requiere_conexion)
    val mensajeReglas = stringResource(R.string.canje_reglas_denegadas)
    val mensajeError = stringResource(R.string.canje_error)
    LaunchedEffect(feedbackEvents) {
        feedbackEvents?.collect { evento ->
            val mensaje = when (evento) {
                CanjeFeedback.SaldoInsuficiente -> mensajeInsuficiente
                CanjeFeedback.RecompensaNoDisponible -> mensajeSinPremios
                CanjeFeedback.Exitoso -> mensajeExito
                CanjeFeedback.SinConexion -> mensajeSinConexion
                CanjeFeedback.FirestoreNoAutorizado -> mensajeReglas
                CanjeFeedback.Error -> mensajeError
            }
            snackbar.showSnackbar(mensaje, duration = SnackbarDuration.Short)
        }
    }
    Box(modifier = modifier.fillMaxSize()) {
        when (estado) {
            RecompensasUiState.Cargando -> PantallaCargando(Modifier.fillMaxSize())
            RecompensasUiState.Error -> PantallaError(onReintentar, Modifier.fillMaxSize())
            RecompensasUiState.SinConexion -> PantallaSinConexion(onReintentar, Modifier.fillMaxSize())
            is RecompensasUiState.Lista -> ListaRecompensas(
                estado.recompensas,
                onCanjear,
                canjePendiente,
                propiedad,
                preferencias,
                onAplicarCosmetico,
                onQuitarCosmetico,
                Modifier.fillMaxSize(),
            )
        }
        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ListaRecompensas(
    recompensas: List<Recompensa>,
    onCanjear: ((Recompensa) -> Unit)?,
    canjePendiente: PendingRedemption?,
    propiedad: List<CosmeticOwnership>,
    preferencias: CosmeticPreferences,
    onAplicarCosmetico: ((String) -> Unit)?,
    onQuitarCosmetico: ((TipoRecompensa) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val ownedIds = propiedad.mapTo(mutableSetOf()) { it.rewardId }
    val hayPremiosParaCaja = recompensas.any { it.kind.esCosmetico() && it.id !in ownedIds }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("lista_recompensas"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            // FR-019: nada se presenta como un beneficio fuera de la app.
            Text(
                text = stringResource(R.string.recompensas_aclaracion),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (canjePendiente != null) {
            item { CanjePendiente(canjePendiente) }
        }
        items(recompensas, key = { it.id }) { recompensa ->
            TarjetaRecompensa(
                recompensa = recompensa,
                onCanjear = onCanjear,
                canjePendiente = canjePendiente != null,
                poseida = recompensa.id in ownedIds,
                hayPremiosParaCaja = hayPremiosParaCaja,
            )
        }
        item {
            Text(text = stringResource(R.string.recompensas_mis_cosmeticos), style = MaterialTheme.typography.titleMedium)
            if (propiedad.isEmpty()) {
                Text(text = stringResource(R.string.recompensas_sin_cosmeticos), modifier = Modifier.testTag("cosmeticos_vacios"))
            }
        }
        items(propiedad, key = { it.rewardId }) { cosmetico ->
            TarjetaCosmetico(cosmetico, preferencias, onAplicarCosmetico, onQuitarCosmetico)
        }
    }
}

@Composable
private fun CanjePendiente(pendiente: PendingRedemption) {
    Card(modifier = Modifier.fillMaxWidth().testTag("canje_pendiente")) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = stringResource(R.string.canje_pendiente_titulo), style = MaterialTheme.typography.titleMedium)
            Text(text = pendiente.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(
                    R.string.canje_pendiente_detalle,
                    pendiente.pointsDebited,
                    pendiente.costPoints,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun TarjetaRecompensa(
    recompensa: Recompensa,
    onCanjear: ((Recompensa) -> Unit)?,
    canjePendiente: Boolean,
    poseida: Boolean,
    hayPremiosParaCaja: Boolean,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = recompensa.name, style = MaterialTheme.typography.titleMedium)
            Text(text = recompensa.description, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = pluralStringResource(R.plurals.recompensas_costo, recompensa.costPoints, recompensa.costPoints),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            if (poseida) {
                Text(text = stringResource(R.string.recompensas_ya_poseida))
            } else if (onCanjear != null) {
                Button(
                    onClick = { onCanjear(recompensa) },
                    enabled = !canjePendiente && (recompensa.kind != TipoRecompensa.CAJA_SORPRESA || hayPremiosParaCaja),
                    modifier = Modifier.testTag("boton_canjear_${recompensa.id}"),
                ) {
                    Text(if (recompensa.kind == TipoRecompensa.CAJA_SORPRESA && !hayPremiosParaCaja) {
                        stringResource(R.string.recompensas_caja_agotada)
                    } else {
                        stringResource(R.string.canje_confirmar_accion)
                    })
                }
            }
        }
    }
}

@Composable
private fun TarjetaCosmetico(
    cosmetico: CosmeticOwnership,
    preferencias: CosmeticPreferences,
    onAplicar: ((String) -> Unit)?,
    onQuitar: ((TipoRecompensa) -> Unit)?,
) {
    val activo = preferencias.activeCosmetics[cosmetico.kind] == cosmetico.rewardId
    Card(modifier = Modifier.fillMaxWidth().testTag("cosmetico_${cosmetico.rewardId}")) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = cosmetico.name, style = MaterialTheme.typography.titleMedium)
            Text(text = cosmetico.kind.valorAlmacen, style = MaterialTheme.typography.bodySmall)
            if (onAplicar != null && onQuitar != null) {
                Button(
                    onClick = { if (activo) onQuitar(cosmetico.kind) else onAplicar(cosmetico.rewardId) },
                    modifier = Modifier.testTag("boton_${if (activo) "quitar" else "aplicar"}_${cosmetico.rewardId}"),
                ) {
                    Text(stringResource(if (activo) R.string.recompensas_quitar else R.string.recompensas_aplicar))
                }
            }
        }
    }
}
