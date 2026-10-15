package com.desconectado.app.ui.challenges

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.desconectado.app.R
import com.desconectado.app.domain.model.ChallengeResult
import com.desconectado.app.domain.model.CosmeticPreferences
import com.desconectado.app.domain.model.ErrorApp
import com.desconectado.app.domain.model.TipoRecompensa
import com.desconectado.app.ui.components.PantallaCargando
import com.desconectado.app.ui.components.DialogoProcesando
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.util.Locale

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
    var errorSonido by remember(challengeRunId) { mutableStateOf(false) }
    val selectedAnimation = preferenciasCosmeticas.activeCosmetics[TipoRecompensa.ANIMACION_COMPLETADO]
    val resultScale by animateFloatAsState(
        targetValue = if (challengeRunId != null && selectedAnimation != null) 1.04f else 1f,
        label = "animacion_completado",
    )
    LaunchedEffect(challengeRunId, selectedSound) {
        if (challengeRunId != null && selectedSound != null) {
            var tone: ToneGenerator? = null
            try {
                tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 30)
                check(tone.startTone(ToneGenerator.TONE_PROP_ACK, 160)) { "El dispositivo rechazó la reproducción del tono" }
                delay(180)
            } catch (e: CancellationException) {
                throw e
            } catch (e: RuntimeException) {
                Log.e("DesafioAudio", "No se pudo reproducir el sonido de finalización", e)
                errorSonido = true
            } finally {
                try {
                    tone?.release()
                } catch (e: RuntimeException) {
                    Log.e("DesafioAudio", "No se pudo liberar el audio de finalización", e)
                }
            }
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val terminal = estado as? DesafioActivoUiState.Terminado
    val logrosDesbloqueados = terminal?.logrosDesbloqueados.orEmpty()
    val nombresLogros = logrosDesbloqueados.joinToString(", ") { it.definition.name }
    val mensajeLogros = when (logrosDesbloqueados.size) {
        0 -> ""
        1 -> stringResource(R.string.desafio_logro_desbloqueado, nombresLogros)
        else -> stringResource(R.string.desafios_logros_desbloqueados, nombresLogros)
    }
    LaunchedEffect(challengeRunId, mensajeLogros) {
        if (mensajeLogros.isNotBlank()) {
            snackbarHostState.showSnackbar(mensajeLogros, duration = SnackbarDuration.Short)
        }
    }
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
                .then(backgroundColor?.let(Modifier::background) ?: Modifier)
                .then(if (terminal != null) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (estado) {
                DesafioActivoUiState.Cargando -> PantallaCargando()
                DesafioActivoUiState.Iniciando -> ProcesoDesafio(completando = false)
                DesafioActivoUiState.Completando -> ProcesoDesafio(completando = true)
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
                    BarraDesafioActivo(
                        desafio = estado.desafio,
                        segundosRestantes = estado.segundosRestantes,
                        onCancelar = onCancelar,
                    )
                }
                is DesafioActivoUiState.Terminado -> {
                    if (estado.resultado.status == ChallengeResult.Status.COMPLETED) {
                        Text(mensajeResultado(estado.resultado), modifier = Modifier.graphicsLayer {
                            scaleX = resultScale
                            scaleY = resultScale
                        })
                    } else {
                        ResultadoNoCompletado(estado.resultado)
                    }
                    if (errorSonido) {
                        Text(stringResource(R.string.desafio_error_sonido), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (estado.resultado.pointsMultiplier > 1.0) {
                        Text(stringResource(
                            R.string.desafio_bonus_diario,
                            estado.resultado.streakBonusPoints,
                            estado.resultado.dailyBasePoints,
                        ))
                        Text(
                            stringResource(
                                R.string.desafio_multiplicador_aplicado,
                                String.format(Locale.getDefault(), "%.3f", estado.resultado.pointsMultiplier),
                            ),
                        )
                    }
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
                        Button(
                            onClick = onVolverCatalogo,
                            modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth()
                                .testTag("boton_volver_catalogo"),
                        ) {
                            Text(stringResource(R.string.desafio_volver_catalogo))
                        }
                    }
                }
                DesafioActivoUiState.SinConexion -> Text(stringResource(R.string.desafio_requiere_conexion))
                is DesafioActivoUiState.Error -> {
                    Text(if (estado.alFinalizar) stringResource(R.string.desafio_error_cierre) else mensajeError(estado.causa))
                    if (estado.alFinalizar) {
                        Button(onClick = onFinalizar, modifier = Modifier.testTag("boton_reintentar_cierre")) {
                            Text(stringResource(R.string.accion_reintentar))
                        }
                    }
                }
            }
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ResultadoNoCompletado(resultado: ChallengeResult) {
    val cancelado = resultado.status == ChallengeResult.Status.CANCELLED
    val invalidado = resultado.status == ChallengeResult.Status.INVALIDATED
    val sinConexion = invalidado && resultado.offlineSeconds > 300
    val permisoRevocado = invalidado && resultado.invalidationReason == "usage_access_revoked"
    val usoApps = resultado.invalidationReason == "social_app_used" || resultado.measuredSocialSeconds > 0
    val colores = MaterialTheme.colorScheme
    val icono = when {
        cancelado -> Icons.Filled.Pause
        sinConexion -> Icons.Filled.WifiOff
        usoApps -> Icons.Filled.PhoneAndroid
        else -> Icons.Filled.Info
    }
    val motivo = when {
        cancelado -> stringResource(R.string.desafio_motivo_cancelado)
        permisoRevocado -> stringResource(R.string.desafio_motivo_permiso_revocado)
        invalidado && resultado.invalidationReason == "social_app_used" ->
            stringResource(R.string.desafio_motivo_app_detectada)
        sinConexion -> stringResource(
            R.string.desafio_motivo_sin_conexion, resultado.offlineSeconds / 60, resultado.offlineSeconds % 60,
        )
        invalidado -> stringResource(R.string.desafio_motivo_no_verificable)
        resultado.measuredSocialSeconds > 0 -> stringResource(
            R.string.desafio_motivo_uso_apps, resultado.measuredSocialSeconds,
        )
        java.time.Duration.between(resultado.startedAt, resultado.finishedAt).seconds < resultado.durationSeconds ->
            stringResource(R.string.desafio_motivo_tiempo_incompleto)
        else -> stringResource(R.string.desafio_motivo_no_verificable)
    }
    Surface(
        modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth().testTag("resultado_desafio_no_completado"),
        shape = RoundedCornerShape(28.dp),
        color = colores.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Surface(shape = CircleShape, color = colores.secondaryContainer) {
                Icon(
                    icono, contentDescription = null, tint = colores.onSecondaryContainer,
                    modifier = Modifier.padding(20.dp).size(40.dp),
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    mensajeResultado(resultado),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    resultado.challengeTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colores.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            Surface(shape = RoundedCornerShape(20.dp), color = colores.surface) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = colores.secondary, modifier = Modifier.size(20.dp))
                        Text(
                            stringResource(R.string.desafio_motivo_titulo),
                            style = MaterialTheme.typography.labelLarge,
                            color = colores.secondary,
                        )
                    }
                    Text(motivo, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("motivo_desafio"))
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, tint = colores.primary, modifier = Modifier.size(24.dp))
                Text(
                    stringResource(when {
                        cancelado -> R.string.desafio_consejo_cancelado
                        permisoRevocado -> R.string.desafio_consejo_permiso
                        sinConexion -> R.string.desafio_consejo_conexion
                        usoApps -> R.string.desafio_consejo_apps
                        invalidado -> R.string.desafio_consejo_verificacion
                        else -> R.string.desafio_consejo_reintento
                    }),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colores.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun DialogoDesafioEnProceso(completando: Boolean) {
    DialogoProcesando(
        mensaje = stringResource(if (completando) R.string.desafio_completando else R.string.desafio_iniciando),
        detalle = stringResource(if (completando) R.string.desafio_completando_detalle else R.string.desafio_iniciando_detalle),
        modifier = Modifier.testTag("pantalla_desafio_en_proceso"),
    )
}

@Composable
private fun ProcesoDesafio(completando: Boolean) {
    PantallaCargando(
        modifier = Modifier.padding(24.dp),
        mensaje = stringResource(if (completando) R.string.desafio_completando else R.string.desafio_iniciando),
        detalle = stringResource(
            if (completando) R.string.desafio_completando_detalle else R.string.desafio_iniciando_detalle,
        ),
    )
}

@Composable
fun BarraDesafioActivo(
    desafio: com.desconectado.app.domain.model.ActiveChallenge,
    segundosRestantes: Long,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).testTag("barra_desafio_activo")) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(desafio.challengeTitle, modifier = Modifier.weight(1f), maxLines = 2, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = String.format(java.util.Locale.ROOT, "%02d:%02d", segundosRestantes / 60, segundosRestantes % 60),
                modifier = Modifier.widthIn(min = 64.dp).testTag("tiempo_restante_desafio"),
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
            )
            IconButton(onClick = onCancelar, modifier = Modifier.testTag("boton_cancelar_desafio")) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.desafio_cancelar))
            }
        }
        LinearProgressIndicator(
            progress = { (1f - segundosRestantes.toFloat() / desafio.durationSeconds.coerceAtLeast(1)).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
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
    ChallengeResult.Status.COMPLETED -> if (resultado.pointsMultiplier > 1.0) {
        stringResource(R.string.desafio_cumplido_multiplicador, resultado.pointsAwarded)
    } else {
        stringResource(R.string.desafio_cumplido, resultado.pointsAwarded)
    }
    ChallengeResult.Status.CANCELLED -> stringResource(R.string.desafio_cancelado_titulo)
    ChallengeResult.Status.INVALIDATED -> stringResource(R.string.desafio_invalidado_titulo)
    ChallengeResult.Status.FAILED -> stringResource(R.string.desafio_no_cumplido_titulo)
}
