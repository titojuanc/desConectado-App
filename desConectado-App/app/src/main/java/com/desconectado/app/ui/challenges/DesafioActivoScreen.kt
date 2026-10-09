package com.desconectado.app.ui.challenges

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.desconectado.app.R
import com.desconectado.app.domain.model.ChallengeResult
import com.desconectado.app.domain.model.CosmeticPreferences
import com.desconectado.app.domain.model.ErrorApp
import com.desconectado.app.domain.model.TipoRecompensa
import android.media.AudioManager
import android.media.ToneGenerator
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
    onSeleccionarCalificacion: (Int) -> Unit,
    onCalificar: () -> Unit,
    onVolverCatalogo: () -> Unit,
    preferenciasCosmeticas: CosmeticPreferences = CosmeticPreferences(),
    modifier: Modifier = Modifier,
) {
    val backgroundId = preferenciasCosmeticas.activeCosmetics[TipoRecompensa.FONDO_ENFOQUE]
    val backgroundColor = if (estado is DesafioActivoUiState.Activo) {
        colorFondoDesafio(backgroundId)
    } else {
        null
    }
    val challengeRunId = (estado as? DesafioActivoUiState.Terminado)
        ?.takeIf { it.resultado.status == ChallengeResult.Status.COMPLETED }
        ?.resultado?.challengeRunId
    val selectedSound = preferenciasCosmeticas.activeCosmetics[TipoRecompensa.SONIDO_COMPLETADO]
    val selectedAnimation = preferenciasCosmeticas.activeCosmetics[TipoRecompensa.ANIMACION_COMPLETADO]
    val resultScale by animateFloatAsState(
        targetValue = if (challengeRunId != null && selectedAnimation != null) 1.04f else 1f,
        label = "animacion_completado",
    )
    LaunchedEffect(challengeRunId, selectedSound) {
        if (challengeRunId != null && selectedSound != null) {
            val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 30)
            try {
                tone.startTone(ToneGenerator.TONE_PROP_ACK, 160)
                delay(180)
            } finally {
                tone.release()
            }
        }
    }
    Column(
        modifier = modifier.fillMaxSize()
            .then(backgroundColor?.let(Modifier::background) ?: Modifier)
            .padding(24.dp),
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
                Text(mensajeResultado(estado.resultado), modifier = Modifier.graphicsLayer {
                    scaleX = resultScale
                    scaleY = resultScale
                })
                if (estado.resultado.status == ChallengeResult.Status.COMPLETED && estado.ratingStars == null) {
                    Text(stringResource(R.string.desafio_rating_titulo))
                    FilaEstrellas(estado.ratingSeleccionado, onSeleccionarCalificacion)
                    if (estado.ratingError) Text(stringResource(R.string.desafio_rating_error))
                    Button(
                        onClick = onCalificar,
                        enabled = estado.ratingSeleccionado in 1..5 && !estado.ratingGuardando,
                        modifier = Modifier.testTag("boton_confirmar_calificacion"),
                    ) {
                        Text(stringResource(R.string.desafio_rating_guardar))
                    }
                } else if (estado.ratingStars != null) {
                    Text(stringResource(R.string.desafio_rating_guardada, estado.ratingStars))
                }
                if (estado.resultado.status != ChallengeResult.Status.COMPLETED || estado.ratingStars != null) {
                    Button(onClick = onVolverCatalogo, modifier = Modifier.testTag("boton_volver_catalogo")) {
                        Text(stringResource(R.string.desafio_volver_catalogo))
                    }
                }
            }
            DesafioActivoUiState.SinConexion -> Text(stringResource(R.string.desafio_requiere_conexion))
            is DesafioActivoUiState.Error -> Text(mensajeError(estado.causa))
        }
    }
}

private fun colorFondoDesafio(backgroundId: String?): Color? = when (backgroundId) {
    "background-montanas" -> Color(0xFFE8F0E5)
    "background-noche-estrellada" -> Color(0xFFE8EDF8)
    "background-amanecer" -> Color(0xFFFFF1DF)
    else -> null
}

@Composable
private fun FilaEstrellas(seleccionadas: Int, onSeleccionar: (Int) -> Unit) {
    Row(modifier = Modifier.testTag("rating_estrellas")) {
        (1..5).forEach { estrella ->
            IconButton(
                onClick = { onSeleccionar(estrella) },
                modifier = Modifier.testTag("boton_rating_$estrella"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = stringResource(R.string.desafio_rating_estrella_desc, estrella),
                    tint = if (estrella <= seleccionadas) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
