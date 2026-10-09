package com.desconectado.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
        Text(text = stringResource(titleResource), style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth().testTag("selector_meta_semanal"),
            ) {
                Text(selectedMinutes?.let { stringResource(R.string.perfil_meta_opcion, it) }
                    ?: stringResource(R.string.perfil_meta_elegir))
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