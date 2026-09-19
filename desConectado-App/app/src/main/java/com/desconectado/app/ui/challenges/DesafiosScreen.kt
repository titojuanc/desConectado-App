package com.desconectado.app.ui.challenges

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.desconectado.app.R
import com.desconectado.app.domain.etiquetaDificultad
import com.desconectado.app.domain.formatearDuracion
import com.desconectado.app.domain.model.Desafio
import com.desconectado.app.domain.model.Dificultad
import com.desconectado.app.ui.components.PantallaCargando
import com.desconectado.app.ui.components.PantallaError
import com.desconectado.app.ui.components.PantallaSinConexion
import com.desconectado.app.ui.theme.DificultadDificil
import com.desconectado.app.ui.theme.DificultadFacil
import com.desconectado.app.ui.theme.DificultadNormal

/** Catálogo de desafíos: solo se consulta, no se pueden iniciar (FR-017). */
@Composable
fun DesafiosScreen(
    estado: DesafiosUiState,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (estado) {
        DesafiosUiState.Cargando -> PantallaCargando(modifier)
        DesafiosUiState.Error -> PantallaError(onReintentar, modifier)
        DesafiosUiState.SinConexion -> PantallaSinConexion(onReintentar, modifier)
        is DesafiosUiState.Lista -> ListaDesafios(estado.desafios, modifier)
    }
}

@Composable
private fun ListaDesafios(desafios: List<Desafio>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("lista_desafios"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(desafios, key = { it.id }) { desafio -> TarjetaDesafio(desafio) }
    }
}

@Composable
private fun TarjetaDesafio(desafio: Desafio) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EtiquetaDificultad(desafio.difficulty)
                Text(
                    text = pluralStringResource(R.plurals.puntos, desafio.points, desafio.points),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(text = desafio.title, style = MaterialTheme.typography.titleMedium)
            Text(text = desafio.description, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = stringResource(R.string.desafios_duracion, formatearDuracion(desafio.durationMinutes)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** La dificultad se comunica con texto y con color, nunca solo con color. */
@Composable
private fun EtiquetaDificultad(dificultad: Dificultad) {
    val color: Color = when (dificultad) {
        Dificultad.FACIL -> DificultadFacil
        Dificultad.NORMAL -> DificultadNormal
        Dificultad.DIFICIL -> DificultadDificil
    }
    Surface(color = color, contentColor = Color.White, shape = RoundedCornerShape(50)) {
        Text(
            text = etiquetaDificultad(dificultad),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}
