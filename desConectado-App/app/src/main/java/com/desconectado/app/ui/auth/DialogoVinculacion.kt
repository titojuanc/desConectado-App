package com.desconectado.app.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.desconectado.app.R

/** Acciones del diálogo de vinculación, para pasarlas juntas a las pantallas de acceso. */
data class AccionesVinculacion(
    val onEmailChange: (String) -> Unit = {},
    val onPasswordChange: (String) -> Unit = {},
    val onConfirmar: () -> Unit = {},
    val onCancelar: () -> Unit = {},
)

/** Pide la contraseña de la cuenta que ya existe con ese correo para vincularle Google (FR-008). */
@Composable
fun DialogoVinculacion(
    vinculacion: VinculacionPendiente,
    acciones: AccionesVinculacion,
) {
    AlertDialog(
        onDismissRequest = { if (!vinculacion.enviando) acciones.onCancelar() },
        title = { Text(stringResource(R.string.vinculacion_titulo)) },
        text = {
            Column {
                Text(stringResource(R.string.vinculacion_mensaje))
                CampoFormulario(
                    valor = vinculacion.email,
                    onCambio = acciones.onEmailChange,
                    etiqueta = stringResource(R.string.campo_correo),
                    etiquetaPrueba = "campo_correo_vincular",
                    tipoTeclado = KeyboardType.Email,
                    modifier = Modifier.padding(top = 16.dp),
                )
                CampoFormulario(
                    valor = vinculacion.password,
                    onCambio = acciones.onPasswordChange,
                    etiqueta = stringResource(R.string.campo_password),
                    etiquetaPrueba = "campo_password_vincular",
                    tipoTeclado = KeyboardType.Password,
                    oculto = true,
                    accionTeclado = ImeAction.Done,
                    modifier = Modifier.padding(top = 8.dp),
                )
                MensajeErrorEnvio(vinculacion.error)
            }
        },
        confirmButton = {
            TextButton(
                onClick = acciones.onConfirmar,
                enabled = !vinculacion.enviando,
                modifier = Modifier.testTag("boton_vincular"),
            ) {
                Text(stringResource(R.string.vinculacion_confirmar))
            }
        },
        dismissButton = {
            TextButton(
                onClick = acciones.onCancelar,
                enabled = !vinculacion.enviando,
                modifier = Modifier.testTag("boton_cancelar_vinculacion"),
            ) {
                Text(stringResource(R.string.vinculacion_cancelar))
            }
        },
        modifier = Modifier.testTag("dialogo_vinculacion"),
    )
}
