package com.desconectado.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.desconectado.app.R
import com.desconectado.app.domain.formatearFechaDesafio
import com.desconectado.app.domain.formatearPuntos
import com.desconectado.app.domain.model.DesafioHecho
import com.desconectado.app.domain.model.Perfil
import com.desconectado.app.domain.model.RedeemedReward
import com.desconectado.app.ui.components.PantallaCargando
import com.desconectado.app.ui.components.PantallaError
import com.desconectado.app.ui.components.PantallaSinConexion

/**
 * Perfil de la persona: nombre de usuario y correo, sin contraseña (FR-010, FR-021), su saldo de
 * puntos, el historial completo de desafíos y los canjes, solo lectura. Cerrar sesión está
 * disponible en todos los estados, también si el perfil no cargó. Los historiales fallan de forma
 * independiente y no ocultan los datos de la persona.
 */
@Composable
fun PerfilScreen(
    estado: PerfilUiState,
    onReintentar: () -> Unit,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
    desafiosHechos: DesafiosHechosUiState = DesafiosHechosUiState.Lista(emptyList()),
    onReintentarPuntos: () -> Unit = {},
    canjes: CanjesPerfilUiState = CanjesPerfilUiState.Lista(emptyList()),
    onReintentarCanjes: () -> Unit = {},
) {
    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f)) {
            when (estado) {
                PerfilUiState.Cargando -> PantallaCargando()
                PerfilUiState.Error -> PantallaError(onReintentar, mensaje = stringResource(R.string.perfil_error))
                PerfilUiState.SinConexion -> PantallaSinConexion(onReintentar)
                is PerfilUiState.Datos -> DatosPerfil(
                    estado.perfil,
                    desafiosHechos,
                    onReintentarPuntos,
                    canjes,
                    onReintentarCanjes,
                )
            }
        }
        OutlinedButton(
            onClick = onCerrarSesion,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("boton_cerrar_sesion"),
        ) {
            Text(stringResource(R.string.perfil_cerrar_sesion))
        }
    }
}

@Composable
private fun DatosPerfil(
    perfil: Perfil,
    desafiosHechos: DesafiosHechosUiState,
    onReintentarPuntos: () -> Unit,
    canjes: CanjesPerfilUiState,
    onReintentarCanjes: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(text = stringResource(R.string.perfil_titulo), style = MaterialTheme.typography.titleLarge)
        DatoPerfil(
            etiqueta = stringResource(R.string.perfil_etiqueta_username),
            valor = perfil.username,
            etiquetaPrueba = "texto_username",
        )
        DatoPerfil(
            etiqueta = stringResource(R.string.perfil_etiqueta_email),
            valor = perfil.email,
            etiquetaPrueba = "texto_email",
        )
        DatoPerfil(
            etiqueta = stringResource(R.string.perfil_etiqueta_puntos),
            valor = formatearPuntos(perfil.puntos),
            etiquetaPrueba = "puntos_perfil",
        )
        DesafiosHechos(desafiosHechos, onReintentarPuntos)
        CanjesRealizados(canjes, onReintentarCanjes)
    }
}

/** Historial completo de desafíos, con sus propios estados de carga, vacío, error y sin conexión. */
@Composable
private fun DesafiosHechos(estado: DesafiosHechosUiState, onReintentar: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.perfil_desafios_hechos_titulo),
            style = MaterialTheme.typography.titleMedium,
        )
        when (estado) {
            DesafiosHechosUiState.Cargando -> CircularProgressIndicator()
            is DesafiosHechosUiState.Lista ->
                if (estado.desafios.isEmpty()) {
                    Text(
                        text = stringResource(R.string.perfil_sin_desafios_hechos),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag("texto_sin_desafios_hechos"),
                    )
                } else {
                    Column(
                        modifier = Modifier.testTag("lista_desafios_hechos"),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        estado.desafios.forEach { FilaDesafioHecho(it) }
                    }
                }
            DesafiosHechosUiState.Error -> ErrorPuntos(stringResource(R.string.perfil_error_puntos), onReintentar)
            DesafiosHechosUiState.SinConexion -> ErrorPuntos(stringResource(R.string.estado_sin_conexion), onReintentar)
        }
    }
}

@Composable
private fun FilaDesafioHecho(desafio: DesafioHecho) {
    val estado = when (desafio.estado) {
        com.desconectado.app.domain.model.ChallengeResult.Status.COMPLETED -> R.string.perfil_resultado_completado
        com.desconectado.app.domain.model.ChallengeResult.Status.FAILED -> R.string.perfil_resultado_fallido
        com.desconectado.app.domain.model.ChallengeResult.Status.CANCELLED -> R.string.perfil_resultado_cancelado
        com.desconectado.app.domain.model.ChallengeResult.Status.INVALIDATED -> R.string.perfil_resultado_invalidado
    }
    val puntos = if (desafio.estado == com.desconectado.app.domain.model.ChallengeResult.Status.COMPLETED) {
        stringResource(R.string.perfil_desafio_hecho_puntos, formatearPuntos(desafio.puntos))
    } else {
        stringResource(R.string.perfil_historial_puntos, formatearPuntos(desafio.puntos))
    }
    Column {
        Text(text = desafio.titulo, style = MaterialTheme.typography.bodyLarge)
        Text(text = stringResource(estado), style = MaterialTheme.typography.labelMedium)
        Text(
            text = puntos + " · " + formatearFechaDesafio(desafio.fecha),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CanjesRealizados(estado: CanjesPerfilUiState, onReintentar: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.perfil_canjes_titulo),
            style = MaterialTheme.typography.titleMedium,
        )
        when (estado) {
            CanjesPerfilUiState.Cargando -> CircularProgressIndicator()
            is CanjesPerfilUiState.Lista ->
                if (estado.canjes.isEmpty()) {
                    Text(
                        text = stringResource(R.string.perfil_sin_canjes),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag("texto_sin_canjes"),
                    )
                } else {
                    Column(
                        modifier = Modifier.testTag("lista_canjes"),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        estado.canjes.forEach { FilaCanje(it) }
                    }
                }
            CanjesPerfilUiState.Error -> ErrorPuntos(
                stringResource(R.string.perfil_error_canjes),
                onReintentar,
                "boton_reintentar_canjes",
            )
            CanjesPerfilUiState.SinConexion -> ErrorPuntos(
                stringResource(R.string.estado_sin_conexion),
                onReintentar,
                "boton_reintentar_canjes",
            )
        }
    }
}

@Composable
private fun FilaCanje(canje: RedeemedReward) {
    Column(modifier = Modifier.testTag("canje_${canje.redemptionId}")) {
        Text(text = canje.name, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = stringResource(
                R.string.perfil_canje_detalle,
                formatearPuntos(canje.costPoints),
                formatearFechaDesafio(canje.createdAt),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        canje.code?.let { Text(text = stringResource(R.string.perfil_canje_codigo, it)) }
    }
}

@Composable
private fun ErrorPuntos(
    mensaje: String,
    onReintentar: () -> Unit,
    testTag: String = "boton_reintentar_puntos",
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = mensaje, style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onReintentar, modifier = Modifier.testTag(testTag)) {
            Text(stringResource(R.string.accion_reintentar))
        }
    }
}

@Composable
private fun DatoPerfil(etiqueta: String, valor: String, etiquetaPrueba: String) {
    Column {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = valor,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag(etiquetaPrueba),
        )
    }
}
