package com.desconectado.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
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
    onMetaSemanalChange: (Int?) -> Unit = {},
    modifier: Modifier = Modifier,
    vinculacion: AccionesVinculacion = AccionesVinculacion(),
) {
    val puedeEnviar = !estado.enviando && !estado.sinConexion && estado.weeklyGoalMinutes != null

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
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth()) {
        CabeceraAcceso(R.string.registro_titulo)

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

        SelectorMetaSemanal(
            selectedMinutes = estado.weeklyGoalMinutes,
            error = estado.errorMetaSemanal,
            onSelect = onMetaSemanalChange,
            modifier = Modifier.padding(top = 12.dp),
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
            if (estado.enviando) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Text(stringResource(R.string.registro_boton))
        }

        SeparadorAcceso()
        BotonAccesoGoogle(puedeEnviar, onGoogle)

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
}
