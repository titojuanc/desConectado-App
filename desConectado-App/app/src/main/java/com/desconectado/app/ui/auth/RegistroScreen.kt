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
import androidx.compose.material3.OutlinedButton
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
import com.desconectado.app.domain.model.ErrorApp
import com.desconectado.app.ui.components.AvisoSinConexion

/** Pantalla de Registro, sin estado: recibe el estado y las acciones como parámetros. */
@Composable
fun RegistroScreen(
    estado: RegistroUiState,
    onUsernameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRegistrar: () -> Unit,
    onIrAIngreso: () -> Unit,
    onGoogle: () -> Unit,
    modifier: Modifier = Modifier,
    vinculacion: AccionesVinculacion = AccionesVinculacion(),
) {
    val puedeEnviar = !estado.enviando && !estado.sinConexion

    estado.vinculacion?.let { DialogoVinculacion(it, vinculacion) }

    // El correo repetido se muestra junto al campo de correo; el resto, debajo de los campos.
    val correoEnUso = estado.errorEnvio == ErrorApp.CorreoEnUso
    val errorCorreo = when {
        estado.errores.email != null -> stringResource(estado.errores.email.mensajeCorreo())
        correoEnUso -> stringResource(R.string.error_correo_en_uso)
        else -> null
    }

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
            text = stringResource(R.string.registro_titulo),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )

        if (estado.sinConexion) {
            AvisoSinConexion(modifier = Modifier.padding(bottom = 16.dp))
        }

        CampoFormulario(
            valor = estado.username,
            onCambio = onUsernameChange,
            etiqueta = stringResource(R.string.campo_username),
            etiquetaPrueba = "campo_username",
            mensajeError = estado.errores.username?.let { stringResource(it.mensajeUsername()) },
        )
        CampoFormulario(
            valor = estado.email,
            onCambio = onEmailChange,
            etiqueta = stringResource(R.string.campo_correo),
            etiquetaPrueba = "campo_correo",
            mensajeError = errorCorreo,
            tipoTeclado = KeyboardType.Email,
            modifier = Modifier.padding(top = 8.dp),
        )
        CampoFormulario(
            valor = estado.password,
            onCambio = onPasswordChange,
            etiqueta = stringResource(R.string.campo_password),
            etiquetaPrueba = "campo_password",
            mensajeError = estado.errores.password?.let { stringResource(it.mensajePassword()) },
            tipoTeclado = KeyboardType.Password,
            oculto = true,
            accionTeclado = ImeAction.Done,
            alConfirmar = { if (puedeEnviar) onRegistrar() },
            modifier = Modifier.padding(top = 8.dp),
        )

        if (!correoEnUso) {
            MensajeErrorEnvio(estado.errorEnvio, modifier = Modifier.fillMaxWidth())
        }

        Button(
            onClick = onRegistrar,
            enabled = puedeEnviar,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .testTag("boton_registrar"),
        ) {
            Text(stringResource(R.string.registro_boton))
        }

        OutlinedButton(
            onClick = onGoogle,
            enabled = puedeEnviar,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .testTag("boton_google"),
        ) {
            Text(stringResource(R.string.google_continuar))
        }

        TextButton(
            onClick = onIrAIngreso,
            modifier = Modifier
                .padding(top = 8.dp)
                .testTag("enlace_ingreso"),
        ) {
            Text(stringResource(R.string.registro_enlace_ingreso))
        }
    }
}
