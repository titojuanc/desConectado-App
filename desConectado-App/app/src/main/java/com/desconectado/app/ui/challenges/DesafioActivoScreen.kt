package com.desconectado.app.ui.challenges

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.desconectado.app.R
import com.desconectado.app.domain.model.ChallengeResult
import com.desconectado.app.domain.model.ErrorApp
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.delay

@Composable
fun DesafioActivoScreen(
    estado: DesafioActivoUiState,
    onAbrirAjustes: () -> Unit,
    onReintentarPermiso: () -> Unit,
    onActualizar: () -> Unit,
    onFinalizar: () -> Unit,
    onCancelar: () -> Unit,
    onVolverCatalogo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (estado) {
            DesafioActivoUiState.Cargando -> Text(stringResource(R.string.estado_cargando))
            DesafioActivoUiState.SinDesafio -> Text(stringResource(R.string.desafio_sin_activo))
            DesafioActivoUiState.SinPermiso -> {
                Text(stringResource(R.string.desafio_permiso_mensaje))
                Button(onClick = onAbrirAjustes, modifier = Modifier.testTag("boton_abrir_ajustes")) {
                    Text(stringResource(R.string.desafio_abrir_ajustes))
                }
                Button(onClick = onReintentarPermiso, modifier = Modifier.testTag("boton_reintentar_permiso")) {
                    Text(stringResource(R.string.desafio_actualizar))
                }
            }
            is DesafioActivoUiState.Activo -> {
                var ahora by remember(estado.desafio.startedAt) { mutableStateOf(Instant.now()) }
                LaunchedEffect(estado.desafio.startedAt) {
                    while (true) {
                        delay(1000)
                        ahora = Instant.now()
                        onActualizar()
                        if (Duration.between(estado.desafio.startedAt, ahora).seconds >= estado.desafio.durationSeconds) {
                            onFinalizar()
                            break
                        }
                    }
                }
                val transcurrido = Duration.between(estado.desafio.startedAt, ahora).seconds
                    .coerceAtLeast(0)
                    .coerceAtMost(estado.desafio.durationSeconds.toLong())
                Text(estado.desafio.challengeTitle)
                Text(stringResource(R.string.desafio_tiempo_transcurrido, transcurrido, estado.desafio.durationSeconds))
                Text(
                    text = if (estado.desafio.durationSeconds < 60) {
                        stringResource(R.string.desafios_duracion_segundos, estado.desafio.durationSeconds)
                    } else {
                        stringResource(R.string.desafio_tiempo_restante, estado.desafio.durationMinutes)
                    },
                )
                Text(stringResource(R.string.desafio_uso_medido, estado.usoSocialSeconds))
                Button(onClick = onActualizar, modifier = Modifier.testTag("boton_actualizar_desafio")) {
                    Text(stringResource(R.string.desafio_actualizar))
                }
                Button(onClick = onFinalizar, modifier = Modifier.testTag("boton_finalizar_desafio")) {
                    Text(stringResource(R.string.desafio_finalizar))
                }
                Button(onClick = onCancelar, modifier = Modifier.testTag("boton_cancelar_desafio")) {
                    Text(stringResource(R.string.desafio_cancelar))
                }
            }
            is DesafioActivoUiState.Terminado -> {
                Text(mensajeResultado(estado.resultado))
                Button(onClick = onVolverCatalogo, modifier = Modifier.testTag("boton_volver_catalogo")) {
                    Text(stringResource(R.string.desafio_volver_catalogo))
                }
            }
            DesafioActivoUiState.SinConexion -> Text(stringResource(R.string.desafio_requiere_conexion))
            is DesafioActivoUiState.Error -> Text(mensajeError(estado.causa))
        }
    }
}

@Composable
private fun mensajeError(error: ErrorApp): String = when (error) {
    ErrorApp.FirestoreNoAutorizado -> stringResource(R.string.desafio_error_firestore_permiso)
    ErrorApp.DatoNoEncontrado -> stringResource(R.string.desafio_error_dato)
    ErrorApp.SinConexion -> stringResource(R.string.desafio_requiere_conexion)
    else -> stringResource(R.string.desafio_error_inicio)
}

@Composable
private fun mensajeResultado(resultado: ChallengeResult): String = when (resultado.status) {
    ChallengeResult.Status.COMPLETED -> stringResource(R.string.desafio_cumplido, resultado.pointsAwarded)
    ChallengeResult.Status.CANCELLED -> stringResource(R.string.desafio_cancelado)
    ChallengeResult.Status.INVALIDATED -> stringResource(R.string.desafio_invalidado)
    ChallengeResult.Status.FAILED -> stringResource(R.string.desafio_no_cumplido)
}
