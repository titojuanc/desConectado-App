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
    modifier: Modifier = Modifier,
) {
    val snackbar = remember { SnackbarHostState() }
    val mensajeInsuficiente = stringResource(R.string.canje_saldo_insuficiente)
    val mensajeExito = stringResource(R.string.canje_exitoso)
    val mensajeSinConexion = stringResource(R.string.desafio_requiere_conexion)
    val mensajeReglas = stringResource(R.string.canje_reglas_denegadas)
    val mensajeError = stringResource(R.string.canje_error)
    LaunchedEffect(feedbackEvents) {
        feedbackEvents?.collect { evento ->
            val mensaje = when (evento) {
                CanjeFeedback.SaldoInsuficiente -> mensajeInsuficiente
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
    modifier: Modifier = Modifier,
) {
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
            TarjetaRecompensa(recompensa, onCanjear, canjePendiente != null)
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
            if (onCanjear != null) {
                Button(
                    onClick = { onCanjear(recompensa) },
                    enabled = !canjePendiente,
                    modifier = Modifier.testTag("boton_canjear_${recompensa.id}"),
                ) {
                    Text(stringResource(R.string.canje_confirmar_accion))
                }
            }
        }
    }
}
