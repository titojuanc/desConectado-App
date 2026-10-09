package com.desconectado.app.ui.challenges

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
        Text(
            text = stringResource(R.string.desafios_pregunta),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            categorias.forEachIndexed { index, categoria ->
                val testTag = categoria?.let { "filtro_${it.valorAlmacen}" } ?: "filtro_todos"
                FilterChip(
                    selected = estado.categoriaSeleccionada == categoria,
                    onClick = { onCategoriaSeleccionada(categoria) },
                    modifier = Modifier.testTag(testTag),
                    shape = RoundedCornerShape(50),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    label = {
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
        LazyVerticalGrid(
            columns = GridCells.Adaptive(156.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("lista_desafios"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
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
    Card(
        modifier = Modifier.fillMaxWidth().testTag("tarjeta_desafio_${desafio.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Image(
                painter = painterResource(ilustracionDesafio(desafio)),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(1.53f).clip(RoundedCornerShape(10.dp))
                    .testTag("ilustracion_desafio_${desafio.id}"),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EtiquetaDificultad(desafio.difficulty)
                val puntosDescripcion = pluralStringResource(R.plurals.puntos, desafio.points, desafio.points)
                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)),
                    modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = puntosDescripcion },
                ) {
                    Row(modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(desafio.points.toString(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("puntos_desafio_${desafio.id}"))
                    }
                }
            }
            Text(text = desafio.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface)
            Text(text = desafio.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                val segundos = desafio.durationSeconds
                Text(
                    text = if (segundos != null && segundos % 60 != 0) stringResource(R.string.desafios_duracion_segundos, segundos)
                        else formatearDuracion(segundos?.div(60) ?: desafio.durationMinutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("duracion_desafio_${desafio.id}"),
                )
            }
            if (onIniciar != null) {
                Button(
                    onClick = { onIniciar(desafio) },
                    modifier = Modifier.fillMaxWidth().testTag("boton_iniciar_desafio_${desafio.id}"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                ) {
                    Spacer(Modifier.size(18.dp))
                    Text(stringResource(R.string.desafios_comenzar), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

private fun ilustracionDesafio(desafio: Desafio): Int = when {
    desafio.title.contains("bici", ignoreCase = true) -> R.drawable.desafio_bici
    desafio.category == CategoriaDesafio.SOCIALIZAR -> R.drawable.desafio_amigos
    desafio.category == CategoriaDesafio.ENFOCARME || desafio.category == CategoriaDesafio.DESCANSAR -> R.drawable.desafio_leer
    else -> R.drawable.desafio_caminar
}

/** La dificultad se comunica con texto y con color, nunca solo con color. */
@Composable
private fun EtiquetaDificultad(dificultad: Dificultad) {
    val color: Color = when (dificultad) {
        Dificultad.FACIL -> Color(0xFF347B3A)
        Dificultad.NORMAL -> Color(0xFF2374BA)
        Dificultad.DIFICIL -> Color(0xFFAD453F)
    }
    Surface(color = color, contentColor = Color.White, shape = RoundedCornerShape(50)) {
        Text(
            text = etiquetaDificultad(dificultad),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
        )
    }
}
