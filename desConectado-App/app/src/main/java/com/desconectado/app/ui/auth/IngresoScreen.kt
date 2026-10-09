package com.desconectado.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.size
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
import com.desconectado.app.ui.components.AvisoSinConexion

/** Pantalla de Ingreso, sin estado: recibe el estado y las acciones como parámetros. */
@Composable
fun IngresoScreen(
    estado: IngresoUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onIngresar: () -> Unit,
    onIrARegistro: () -> Unit,
    onOlvidePassword: () -> Unit,
    onGoogle: () -> Unit,
    onMetaSemanalChange: (Int?) -> Unit = {},
    modifier: Modifier = Modifier,
    vinculacion: AccionesVinculacion = AccionesVinculacion(),
) {
    val puedeEnviar = !estado.enviando && !estado.sinConexion

    estado.vinculacion?.let { DialogoVinculacion(it, vinculacion) }

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
        CabeceraAcceso(R.string.ingreso_titulo)

        if (estado.sinConexion) {
            AvisoSinConexion(modifier = Modifier.padding(bottom = 16.dp))
        }

        CampoFormulario(
            valor = estado.email,
            onCambio = onEmailChange,
            etiqueta = stringResource(R.string.campo_correo),
            etiquetaPrueba = "campo_correo",
            mensajeError = estado.errorCorreo?.let { stringResource(it.mensajeCorreo()) },
            tipoTeclado = KeyboardType.Email,
        )
        CampoFormulario(
            valor = estado.password,
            onCambio = onPasswordChange,
            etiqueta = stringResource(R.string.campo_password),
            etiquetaPrueba = "campo_password",
            mensajeError = estado.errorPassword?.let { stringResource(it.mensajePassword()) },
            tipoTeclado = KeyboardType.Password,
            oculto = true,
            accionTeclado = ImeAction.Done,
            alConfirmar = { if (puedeEnviar) onIngresar() },
            modifier = Modifier.padding(top = 8.dp),
        )

        MensajeErrorEnvio(estado.errorEnvio, modifier = Modifier.fillMaxWidth())

        Button(
            onClick = onIngresar,
            enabled = puedeEnviar,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .testTag("boton_ingresar"),
        ) {
            if (estado.enviando) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Text(stringResource(R.string.ingreso_boton))
        }

        TextButton(
            onClick = onOlvidePassword,
            modifier = Modifier
                .padding(top = 8.dp)
                .testTag("enlace_olvide_password"),
        ) {
            Text(stringResource(R.string.restablecer_enlace))
        }

        SeparadorAcceso()
        SelectorMetaSemanal(
            selectedMinutes = estado.weeklyGoalMinutes,
            error = estado.errorMetaSemanal,
            onSelect = onMetaSemanalChange,
            titleResource = R.string.ingreso_meta_google_titulo,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        BotonAccesoGoogle(puedeEnviar && estado.weeklyGoalMinutes != null, onGoogle)
        TextButton(
            onClick = onIrARegistro,
            modifier = Modifier.testTag("enlace_registro"),
        ) {
            Text(stringResource(R.string.ingreso_enlace_registro))
        }
        }
    }
}
