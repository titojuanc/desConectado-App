package com.desconectado.app.ui.rewards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.desconectado.app.R
import com.desconectado.app.domain.model.Recompensa
import com.desconectado.app.ui.components.PantallaCargando
import com.desconectado.app.ui.components.PantallaError
import com.desconectado.app.ui.components.PantallaSinConexion

/** Catálogo de recompensas: solo se consulta, no hay acción de canje (FR-020). */
@Composable
fun RecompensasScreen(
    estado: RecompensasUiState,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (estado) {
        RecompensasUiState.Cargando -> PantallaCargando(modifier)
        RecompensasUiState.Error -> PantallaError(onReintentar, modifier)
        RecompensasUiState.SinConexion -> PantallaSinConexion(onReintentar, modifier)
        is RecompensasUiState.Lista -> ListaRecompensas(estado.recompensas, modifier)
    }
}

@Composable
private fun ListaRecompensas(recompensas: List<Recompensa>, modifier: Modifier = Modifier) {
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
        items(recompensas, key = { it.id }) { recompensa -> TarjetaRecompensa(recompensa) }
    }
}

@Composable
private fun TarjetaRecompensa(recompensa: Recompensa) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = recompensa.name, style = MaterialTheme.typography.titleMedium)
            Text(text = recompensa.description, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = pluralStringResource(R.plurals.recompensas_costo, recompensa.costPoints, recompensa.costPoints),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
