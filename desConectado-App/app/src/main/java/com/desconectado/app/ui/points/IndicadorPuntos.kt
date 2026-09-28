package com.desconectado.app.ui.points

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.desconectado.app.R
import com.desconectado.app.domain.formatearPuntos

/**
 * Ícono pequeño con el saldo de puntos, arriba a la izquierda de cada pestaña (FR-028). Sin saldo
 * conocido (cargando, sin conexión o con error) muestra un guion, nunca un 0 inventado (FR-033).
 * Tocarlo lleva al Perfil; no ofrece ninguna acción sobre los puntos (FR-032).
 */
@Composable
fun IndicadorPuntos(
    estado: SaldoUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val texto = if (estado is SaldoUiState.Disponible) {
        formatearPuntos(estado.puntos)
    } else {
        stringResource(R.string.puntos_no_disponible)
    }
    val descripcion = if (estado is SaldoUiState.Disponible) {
        stringResource(R.string.puntos_indicador_descripcion, texto)
    } else {
        stringResource(R.string.puntos_indicador_no_disponible_descripcion)
    }

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .semantics(mergeDescendants = true) { contentDescription = descripcion }
            .testTag("indicador_puntos"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Text(
            text = texto,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
