package com.desconectado.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.desconectado.app.R

@Composable
fun TutorialPermisoUsoDialog(
    onDismiss: () -> Unit,
    onAbrirPermisosUso: () -> Unit,
    onAbrirInformacionApp: () -> Unit,
    tienePermisoUso: () -> Boolean,
) {
    var paso by remember { mutableIntStateOf(0) }
    var permisoPendiente by remember { mutableStateOf(false) }
    val titulo = when (paso) {
        0 -> R.string.tutorial_permiso_paso_1_titulo
        1 -> R.string.tutorial_permiso_paso_2_titulo
        else -> R.string.tutorial_permiso_paso_3_titulo
    }
    val descripcion = when (paso) {
        0 -> R.string.tutorial_permiso_paso_1_descripcion
        1 -> R.string.tutorial_permiso_paso_2_descripcion
        else -> R.string.tutorial_permiso_paso_3_descripcion
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titulo)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(R.string.tutorial_permiso_paso_numero, paso + 1),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                LinearProgressIndicator(
                    progress = { (paso + 1) / 3f },
                    modifier = Modifier.fillMaxWidth().testTag("tutorial_permiso_progreso"),
                )
                Text(stringResource(descripcion), modifier = Modifier.testTag("tutorial_permiso_descripcion"))
                if (paso == 2) {
                    Button(
                        onClick = {
                            permisoPendiente = false
                            onAbrirPermisosUso()
                        },
                        modifier = Modifier.fillMaxWidth().testTag("tutorial_permiso_abrir_ajustes_final"),
                    ) {
                        Text(stringResource(R.string.tutorial_permiso_abrir_ajustes))
                    }
                }
                if (permisoPendiente) {
                    Text(
                        stringResource(R.string.tutorial_permiso_aun_denegado),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("tutorial_permiso_error"),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    permisoPendiente = false
                    when (paso) {
                        0 -> {
                            onAbrirPermisosUso()
                            paso = 1
                        }
                        1 -> {
                            onAbrirInformacionApp()
                            paso = 2
                        }
                        else -> if (tienePermisoUso()) onDismiss() else permisoPendiente = true
                    }
                },
                modifier = Modifier.testTag("tutorial_permiso_accion_principal"),
            ) {
                val accion = when (paso) {
                    0 -> R.string.tutorial_permiso_abrir_ajustes
                    1 -> R.string.tutorial_permiso_abrir_informacion
                    else -> R.string.tutorial_permiso_comprobar
                }
                Text(stringResource(accion))
            }
        },
        dismissButton = {
            Row {
                if (paso > 0) {
                    TextButton(onClick = { paso--; permisoPendiente = false }) {
                        Text(stringResource(R.string.tutorial_permiso_anterior))
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.testTag("tutorial_permiso_cerrar")) {
                    Text(stringResource(R.string.tutorial_permiso_cerrar))
                }
            }
        },
    )
}