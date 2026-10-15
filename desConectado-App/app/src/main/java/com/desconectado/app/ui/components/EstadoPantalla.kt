package com.desconectado.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.desconectado.app.R

/** Estado de carga a pantalla completa. */
@Composable
fun PantallaCargando(
    modifier: Modifier = Modifier,
    mensaje: String = stringResource(R.string.estado_cargando),
    detalle: String? = null,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(modifier = Modifier.testTag("indicador_cargando"))
        Text(
            text = mensaje,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 16.dp),
            textAlign = TextAlign.Center,
        )
        if (detalle != null) {
            Text(
                text = detalle,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun DialogoProcesando(mensaje: String, detalle: String, modifier: Modifier = Modifier) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(modifier = modifier.fillMaxSize()) {
            PantallaCargando(
                modifier = Modifier.padding(24.dp),
                mensaje = mensaje,
                detalle = detalle,
            )
        }
    }
}

/** Sección sin conexión: aviso claro de que se requiere conexión, con botón para reintentar (FR-011). */
@Composable
fun PantallaSinConexion(onReintentar: () -> Unit, modifier: Modifier = Modifier) {
    PantallaError(
        onReintentar = onReintentar,
        modifier = modifier,
        mensaje = stringResource(R.string.estado_sin_conexion),
    )
}

/** Estado de error a pantalla completa, con botón para reintentar. */
@Composable
fun PantallaError(
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier,
    mensaje: String = stringResource(R.string.estado_error),
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = mensaje,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onReintentar,
            modifier = Modifier
                .padding(top = 16.dp)
                .testTag("boton_reintentar"),
        ) {
            Text(stringResource(R.string.accion_reintentar))
        }
    }
}
