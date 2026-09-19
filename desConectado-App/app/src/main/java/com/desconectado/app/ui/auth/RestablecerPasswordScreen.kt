package com.desconectado.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.desconectado.app.R
import com.desconectado.app.ui.components.AvisoSinConexion

/** Pantalla de Restablecer contraseña, sin estado (FR-025, FR-026). */
@Composable
fun RestablecerPasswordScreen(
    estado: RestablecerPasswordUiState,
    onEmailChange: (String) -> Unit,
    onEnviar: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val puedeEnviar = !estado.enviando && !estado.sinConexion

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.restablecer_titulo),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = stringResource(R.string.restablecer_explicacion),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )

        if (estado.sinConexion) {
            AvisoSinConexion(modifier = Modifier.padding(bottom = 16.dp))
        }

        CampoFormulario(
            valor = estado.email,
            onCambio = onEmailChange,
            etiqueta = stringResource(R.string.restablecer_campo_correo),
            etiquetaPrueba = "campo_correo_restablecer",
            mensajeError = estado.errorCorreo?.let { stringResource(it.mensajeCorreo()) },
            tipoTeclado = KeyboardType.Email,
            accionTeclado = ImeAction.Done,
            alConfirmar = { if (puedeEnviar) onEnviar() },
        )

        if (estado.confirmado) {
            Text(
                text = stringResource(R.string.restablecer_confirmacion),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .testTag("mensaje_restablecimiento_enviado"),
            )
        }
        MensajeErrorEnvio(estado.errorEnvio, modifier = Modifier.fillMaxWidth())

        Button(
            onClick = onEnviar,
            enabled = puedeEnviar,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .testTag("boton_enviar_restablecimiento"),
        ) {
            Text(stringResource(R.string.restablecer_boton))
        }

        TextButton(
            onClick = onVolver,
            modifier = Modifier
                .padding(top = 8.dp)
                .testTag("enlace_volver_ingreso"),
        ) {
            Text(stringResource(R.string.restablecer_volver))
        }
    }
}
