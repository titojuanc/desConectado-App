package com.desconectado.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Icon
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.ui.Alignment
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.desconectado.app.R
import com.desconectado.app.domain.model.UserPreferences

@Composable
fun SelectorMetaSemanal(
    selectedMinutes: Int?,
    error: Boolean,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    titleResource: Int = R.string.registro_meta_titulo,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = stringResource(titleResource), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth().testTag("selector_meta_semanal"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            ) {
                Icon(Icons.Outlined.Flag, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(selectedMinutes?.let { stringResource(R.string.perfil_meta_opcion, it) }
                    ?: stringResource(R.string.perfil_meta_elegir), modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ExpandMore, contentDescription = null, modifier = Modifier.size(20.dp))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                (UserPreferences.META_MINIMA..UserPreferences.META_MAXIMA step UserPreferences.INCREMENTO_META)
                    .forEach { minutes ->
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.perfil_meta_opcion, minutes)) },
                            onClick = { onSelect(minutes); expanded = false },
                        )
                    }
            }
        }
        if (error) Text(text = stringResource(R.string.registro_meta_error), color = MaterialTheme.colorScheme.error)
    }
}