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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.desconectado.app.R
import com.desconectado.app.domain.etiquetaDificultad
import com.desconectado.app.domain.formatearDuracion
import com.desconectado.app.domain.model.Desafio
import com.desconectado.app.domain.model.CategoriaDesafio
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
    onIniciar: ((Desafio) -> Unit)? = null,
    onCategoriaSeleccionada: (CategoriaDesafio?) -> Unit = {},
    iconPackId: String? = null,
    modifier: Modifier = Modifier,
) {
    when (estado) {
        DesafiosUiState.Cargando -> PantallaCargando(modifier)
        DesafiosUiState.Error -> PantallaError(onReintentar, modifier)
        DesafiosUiState.SinConexion -> PantallaSinConexion(onReintentar, modifier)
        is DesafiosUiState.Lista -> ListaDesafios(estado, onIniciar, onCategoriaSeleccionada, iconPackId, modifier)
    }
}

@Composable
private fun ListaDesafios(
    estado: DesafiosUiState.Lista,
    onIniciar: ((Desafio) -> Unit)?,
    onCategoriaSeleccionada: (CategoriaDesafio?) -> Unit,
    iconPackId: String?,
    modifier: Modifier = Modifier,
) {
    val categorias = listOf(null) + CategoriaDesafio.entries
    val etiquetas = listOf(
        R.string.desafios_categoria_todos,
        R.string.desafios_categoria_moverme,
        R.string.desafios_categoria_enfocarme,
        R.string.desafios_categoria_socializar,
        R.string.desafios_categoria_descansar,
    )
    Column(modifier = modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = categorias.indexOf(estado.categoriaSeleccionada),
            edgePadding = 8.dp,
        ) {
            categorias.forEachIndexed { index, categoria ->
                val testTag = categoria?.let { "filtro_${it.valorAlmacen}" } ?: "filtro_todos"
                Tab(
                    selected = estado.categoriaSeleccionada == categoria,
                    onClick = { onCategoriaSeleccionada(categoria) },
                    modifier = Modifier.testTag(testTag),
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            categoria?.let { Icon(iconoCategoria(it, iconPackId), contentDescription = null) }
                            if (categoria == null && iconPackId == "icon-pack-minimal") {
                                Icon(Icons.Outlined.FilterAlt, contentDescription = null)
                            }
                            Text(stringResource(etiquetas[index]))
                        }
                    },
                )
            }
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("lista_desafios"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(estado.desafios, key = { it.id }) { desafio -> TarjetaDesafio(desafio, onIniciar) }
        }
    }
}

private fun iconoCategoria(categoria: CategoriaDesafio, iconPackId: String?): ImageVector = when (iconPackId) {
    "icon-pack-minimal" -> when (categoria) {
        CategoriaDesafio.MOVERME -> Icons.Outlined.Widgets
        CategoriaDesafio.ENFOCARME -> Icons.Outlined.FilterAlt
        CategoriaDesafio.SOCIALIZAR -> Icons.Outlined.Forum
        CategoriaDesafio.DESCANSAR -> Icons.Outlined.SelfImprovement
    }
    "icon-pack-naturaleza" -> when (categoria) {
        CategoriaDesafio.MOVERME -> Icons.AutoMirrored.Filled.DirectionsWalk
        CategoriaDesafio.ENFOCARME -> Icons.Filled.CenterFocusStrong
        CategoriaDesafio.SOCIALIZAR -> Icons.Filled.Groups
        CategoriaDesafio.DESCANSAR -> Icons.Filled.Spa
    }
    else -> when (categoria) {
        CategoriaDesafio.MOVERME -> Icons.AutoMirrored.Filled.DirectionsWalk
        CategoriaDesafio.ENFOCARME -> Icons.Filled.CenterFocusStrong
        CategoriaDesafio.SOCIALIZAR -> Icons.Filled.Groups
        CategoriaDesafio.DESCANSAR -> Icons.Filled.Park
    }
}

@Composable
private fun TarjetaDesafio(desafio: Desafio, onIniciar: ((Desafio) -> Unit)?) {
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
                text = desafio.durationSeconds?.let { stringResource(R.string.desafios_duracion_segundos, it) }
                    ?: stringResource(R.string.desafios_duracion, formatearDuracion(desafio.durationMinutes)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (onIniciar != null) {
                Button(
                    onClick = { onIniciar(desafio) },
                    modifier = Modifier.testTag("boton_iniciar_desafio_${desafio.id}"),
                ) {
                    Text(stringResource(R.string.desafio_iniciar))
                }
            }
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
