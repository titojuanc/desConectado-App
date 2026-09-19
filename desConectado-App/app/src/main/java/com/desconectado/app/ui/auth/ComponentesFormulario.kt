package com.desconectado.app.ui.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import com.desconectado.app.domain.model.ErrorApp

/** Campo de texto de los formularios de acceso, con su mensaje de error junto al campo (FR-002). */
@Composable
fun CampoFormulario(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    etiquetaPrueba: String,
    modifier: Modifier = Modifier,
    mensajeError: String? = null,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    oculto: Boolean = false,
    accionTeclado: ImeAction = ImeAction.Next,
    alConfirmar: () -> Unit = {},
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        isError = mensajeError != null,
        supportingText = mensajeError?.let { { Text(it) } },
        singleLine = true,
        visualTransformation = if (oculto) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = tipoTeclado, imeAction = accionTeclado),
        keyboardActions = KeyboardActions(onDone = { alConfirmar() }),
        modifier = modifier
            .fillMaxWidth()
            .testTag(etiquetaPrueba),
    )
}

/** Mensaje de error de una petición, si corresponde mostrarlo. */
@Composable
fun MensajeErrorEnvio(error: ErrorApp?, modifier: Modifier = Modifier) {
    val recurso = error?.mensajeEnvio() ?: return
    Text(
        text = stringResource(recurso),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier.padding(top = 4.dp),
    )
}
