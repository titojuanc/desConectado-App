package com.desconectado.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import com.desconectado.app.R
import com.desconectado.app.domain.formatearFechaDesafio
import com.desconectado.app.domain.formatearDuracion
import com.desconectado.app.domain.formatearPuntos
import com.desconectado.app.domain.model.DesafioHecho
import com.desconectado.app.domain.model.AchievementProgress
import com.desconectado.app.domain.model.Perfil
import com.desconectado.app.domain.model.CosmeticPreferences
import com.desconectado.app.domain.model.RedeemedReward
import com.desconectado.app.domain.model.TipoRecompensa
import com.desconectado.app.domain.model.UserPreferences
import com.desconectado.app.domain.ProgressMetrics
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
    proximoVencimiento: ProximoVencimientoUiState = ProximoVencimientoUiState.SinVencimientos,
    onReintentarVencimiento: () -> Unit = {},
    progreso: ProgresoUiState = ProgresoUiState.Oculto,
    onReintentarProgreso: () -> Unit = {},
    preferenciasCosmeticas: CosmeticPreferences = CosmeticPreferences(),
    edicionNombre: EdicionNombreUiState = EdicionNombreUiState(),
    onEditarNombre: () -> Unit = {},
    onCambiarNombre: (String) -> Unit = {},
    onGuardarNombre: () -> Unit = {},
    onCancelarEdicionNombre: () -> Unit = {},
    preferenciasUsuario: PreferenciasPerfilUiState = PreferenciasPerfilUiState.Oculto,
    onReintentarPreferencias: () -> Unit = {},
    onGuardarMetaSemanal: (Int) -> Unit = {},
    onConfigurarNotificaciones: (Boolean) -> Unit = {},
    onAbrirAjustesPrivacidad: () -> Unit = {},
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
                    proximoVencimiento,
                    onReintentarVencimiento,
                    progreso,
                    onReintentarProgreso,
                    preferenciasCosmeticas,
                    edicionNombre,
                    onEditarNombre,
                    onCambiarNombre,
                    onGuardarNombre,
                    onCancelarEdicionNombre,
                    preferenciasUsuario,
                    onReintentarPreferencias,
                    onGuardarMetaSemanal,
                    onConfigurarNotificaciones,
                    onAbrirAjustesPrivacidad,
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
    proximoVencimiento: ProximoVencimientoUiState,
    onReintentarVencimiento: () -> Unit,
    progreso: ProgresoUiState,
    onReintentarProgreso: () -> Unit,
    preferenciasCosmeticas: CosmeticPreferences,
    edicionNombre: EdicionNombreUiState,
    onEditarNombre: () -> Unit,
    onCambiarNombre: (String) -> Unit,
    onGuardarNombre: () -> Unit,
    onCancelarEdicionNombre: () -> Unit,
    preferenciasUsuario: PreferenciasPerfilUiState,
    onReintentarPreferencias: () -> Unit,
    onGuardarMetaSemanal: (Int) -> Unit,
    onConfigurarNotificaciones: (Boolean) -> Unit,
    onAbrirAjustesPrivacidad: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(text = stringResource(R.string.perfil_titulo), style = MaterialTheme.typography.titleLarge)
        AvatarPerfil(perfil.username, preferenciasCosmeticas)
        NombrePerfil(
            username = perfil.username,
            estado = edicionNombre,
            onEditar = onEditarNombre,
            onCambiar = onCambiarNombre,
            onGuardar = onGuardarNombre,
            onCancelar = onCancelarEdicionNombre,
        )
        DatoPerfil(
            etiqueta = stringResource(R.string.perfil_etiqueta_email),
            valor = perfil.email,
            etiquetaPrueba = "texto_email",
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (preferenciasCosmeticas.activeCosmetics[TipoRecompensa.ICONO_PUNTOS] == "point-icon-estrella-especial") {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp).testTag("icono_puntos_cosmetico"),
                )
            }
            DatoPerfil(
                etiqueta = stringResource(R.string.perfil_etiqueta_puntos),
                valor = formatearPuntos(perfil.puntos),
                etiquetaPrueba = "puntos_perfil",
            )
        }
        ProximoVencimiento(proximoVencimiento, onReintentarVencimiento)
        Progreso(progreso, onReintentarProgreso)
        PreferenciasUsuario(
            estado = preferenciasUsuario,
            onReintentar = onReintentarPreferencias,
            onGuardarMeta = onGuardarMetaSemanal,
            onConfigurarNotificaciones = onConfigurarNotificaciones,
        )
        Privacidad(onAbrirAjustesPrivacidad)
        DesafiosHechos(desafiosHechos, onReintentarPuntos)
        CanjesRealizados(canjes, onReintentarCanjes)
    }
}

@Composable
private fun Privacidad(onAbrirAjustes: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = stringResource(R.string.perfil_privacidad_titulo), style = MaterialTheme.typography.titleMedium)
        Text(text = stringResource(R.string.perfil_privacidad_descripcion), style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = onAbrirAjustes, modifier = Modifier.testTag("boton_ajustes_privacidad")) {
            Text(stringResource(R.string.perfil_privacidad_ajustes))
        }
    }
}

@Composable
private fun PreferenciasUsuario(
    estado: PreferenciasPerfilUiState,
    onReintentar: () -> Unit,
    onGuardarMeta: (Int) -> Unit,
    onConfigurarNotificaciones: (Boolean) -> Unit,
) {
    if (estado == PreferenciasPerfilUiState.Oculto) return
    Column(
        modifier = Modifier.fillMaxWidth().testTag("preferencias_perfil"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = stringResource(R.string.perfil_preferencias_titulo), style = MaterialTheme.typography.titleMedium)
        when (estado) {
            PreferenciasPerfilUiState.Oculto -> Unit
            PreferenciasPerfilUiState.Cargando -> CircularProgressIndicator(modifier = Modifier.testTag("cargando_preferencias"))
            PreferenciasPerfilUiState.Error -> ErrorPuntos(
                stringResource(R.string.perfil_preferencias_error), onReintentar, "boton_reintentar_preferencias",
            )
            PreferenciasPerfilUiState.SinConexion -> ErrorPuntos(
                stringResource(R.string.estado_sin_conexion), onReintentar, "boton_reintentar_preferencias",
            )
            is PreferenciasPerfilUiState.Datos -> ControlesPreferencias(
                estado, onGuardarMeta, onConfigurarNotificaciones,
            )
        }
    }
}

@Composable
private fun ControlesPreferencias(
    estado: PreferenciasPerfilUiState.Datos,
    onGuardarMeta: (Int) -> Unit,
    onConfigurarNotificaciones: (Boolean) -> Unit,
) {
    var metaSeleccionada by remember(estado.preferencias.weeklyGoalMinutes) {
        mutableStateOf(estado.preferencias.weeklyGoalMinutes)
    }
    var menuAbierto by remember { mutableStateOf(false) }
    Text(text = stringResource(R.string.perfil_meta_semanal))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(
            onClick = { menuAbierto = true },
            enabled = !estado.guardando,
            modifier = Modifier.testTag("meta_semanal_selector"),
        ) {
            Text(metaSeleccionada?.let { stringResource(R.string.perfil_meta_opcion, it) }
                ?: stringResource(R.string.perfil_meta_elegir))
        }
        DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
            (UserPreferences.META_MINIMA..UserPreferences.META_MAXIMA step UserPreferences.INCREMENTO_META).forEach { minutes ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.perfil_meta_opcion, minutes)) },
                    onClick = { metaSeleccionada = minutes; menuAbierto = false },
                )
            }
        }
        Button(
            onClick = { metaSeleccionada?.let(onGuardarMeta) },
            enabled = !estado.guardando && metaSeleccionada != null && metaSeleccionada != estado.preferencias.weeklyGoalMinutes,
            modifier = Modifier.testTag("boton_guardar_meta"),
        ) {
            Text(stringResource(R.string.perfil_meta_guardar))
        }
    }
    if (estado.error) Text(stringResource(R.string.perfil_preferencias_error))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = stringResource(R.string.perfil_notificaciones))
        Switch(
            checked = estado.preferencias.notificationsEnabled,
            onCheckedChange = onConfigurarNotificaciones,
            enabled = !estado.guardando,
            modifier = Modifier.testTag("toggle_notificaciones"),
        )
    }
}

@Composable
private fun NombrePerfil(
    username: String,
    estado: EdicionNombreUiState,
    onEditar: () -> Unit,
    onCambiar: (String) -> Unit,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit,
) {
    if (!estado.editando) {
        DatoPerfil(
            etiqueta = stringResource(R.string.perfil_etiqueta_username),
            valor = username,
            etiquetaPrueba = "texto_username",
        )
        TextButton(onClick = onEditar, modifier = Modifier.testTag("boton_editar_nombre")) {
            Text(stringResource(R.string.perfil_editar_nombre))
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = estado.texto,
            onValueChange = onCambiar,
            label = { Text(stringResource(R.string.perfil_etiqueta_username)) },
            singleLine = true,
            isError = estado.error,
            modifier = Modifier.fillMaxWidth().testTag("campo_editar_username"),
        )
        if (estado.error) Text(stringResource(R.string.perfil_error_nombre))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onGuardar, enabled = !estado.guardando, modifier = Modifier.testTag("boton_guardar_nombre")) {
                Text(stringResource(R.string.perfil_guardar_nombre))
            }
            TextButton(onClick = onCancelar, enabled = !estado.guardando, modifier = Modifier.testTag("boton_cancelar_edicion_nombre")) {
                Text(stringResource(R.string.perfil_cancelar_edicion_nombre))
            }
        }
    }
}

@Composable
private fun AvatarPerfil(nombre: String, preferencias: CosmeticPreferences) {
    val frameId = preferencias.activeCosmetics[TipoRecompensa.MARCO_PERFIL]
    val iniciales = nombre.trim().split(Regex("\\s+")).take(2)
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .joinToString("")
    val frameColor = when (frameId) {
        "profile-frame-naturaleza" -> Color(0xFF3A7D57)
        null -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.primary
    }
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(if (frameId == null) 0.dp else 3.dp, frameColor, CircleShape)
            .testTag(if (frameId == null) "avatar_perfil" else "avatar_perfil_enmarcado"),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = iniciales, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun Progreso(estado: ProgresoUiState, onReintentar: () -> Unit) {
    if (estado == ProgresoUiState.Oculto) return
    Column(
        modifier = Modifier.fillMaxWidth().testTag("resumen_progreso"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = stringResource(R.string.perfil_progreso_titulo), style = MaterialTheme.typography.titleMedium)
        when (estado) {
            ProgresoUiState.Oculto -> Unit
            ProgresoUiState.Cargando -> CircularProgressIndicator(modifier = Modifier.testTag("cargando_progreso"))
            ProgresoUiState.Error -> ErrorPuntos(
                stringResource(R.string.perfil_error_progreso), onReintentar, "boton_reintentar_progreso",
            )
            ProgresoUiState.SinConexion -> ErrorPuntos(
                stringResource(R.string.estado_sin_conexion), onReintentar, "boton_reintentar_progreso",
            )
            is ProgresoUiState.Datos -> ResumenProgreso(estado.metricas, estado.logros)
        }
    }
}

@Composable
private fun ResumenProgreso(metricas: ProgressMetrics, logros: List<AchievementProgress>) {
    Text(text = pluralStringResource(
        R.plurals.perfil_progreso_desafios,
        metricas.desafiosCompletados,
        metricas.desafiosCompletados,
    ))
    Text(text = stringResource(
        R.string.perfil_progreso_tiempo,
        formatearDuracion((metricas.tiempoCompletadoSegundos / 60).toInt()),
    ))
    Text(text = stringResource(R.string.perfil_progreso_racha, metricas.rachaDias))
    Text(text = stringResource(
        R.string.perfil_progreso_semana,
        formatearDuracion((metricas.tiempoSemanalSegundos / 60).toInt()),
    ))
    val metaSemanal = metricas.minutosRestantesMetaSemanal
    Text(
        text = if (metaSemanal == null) stringResource(R.string.perfil_progreso_meta_pendiente)
        else stringResource(R.string.perfil_progreso_meta_restante, metaSemanal),
    )
    Text(text = stringResource(R.string.perfil_logros_titulo), style = MaterialTheme.typography.titleSmall)
    if (logros.isEmpty()) {
        Text(text = stringResource(R.string.perfil_logros_vacios), modifier = Modifier.testTag("logros_vacios"))
    } else {
        logros.forEach { Logro(it) }
    }
}

@Composable
private fun Logro(logro: AchievementProgress) {
    Column(modifier = Modifier.testTag("logro_${logro.definition.id}")) {
        Text(text = logro.definition.name, style = MaterialTheme.typography.bodyLarge)
        Text(text = logro.definition.description, style = MaterialTheme.typography.bodyMedium)
        Text(text = stringResource(R.string.perfil_logro_progreso, logro.progress, logro.threshold))
        if (logro.unlocked) Text(text = stringResource(R.string.perfil_logro_desbloqueado))
    }
}

@Composable
private fun ProximoVencimiento(estado: ProximoVencimientoUiState, onReintentar: () -> Unit) {
    when (estado) {
        ProximoVencimientoUiState.Cargando -> CircularProgressIndicator(modifier = Modifier.testTag("cargando_vencimiento"))
        is ProximoVencimientoUiState.Proximo -> Text(
            text = stringResource(
                R.string.perfil_proximo_vencimiento,
                formatearPuntos(estado.vencimiento.points),
                estado.vencimiento.daysRemaining,
            ),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag("proximo_vencimiento"),
        )
        ProximoVencimientoUiState.SinVencimientos -> Text(
            text = stringResource(R.string.perfil_sin_puntos_por_vencer),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag("sin_puntos_por_vencer"),
        )
        ProximoVencimientoUiState.Error -> ErrorPuntos(
            stringResource(R.string.perfil_error_vencimiento),
            onReintentar,
            "boton_reintentar_vencimiento",
        )
        ProximoVencimientoUiState.SinConexion -> ErrorPuntos(
            stringResource(R.string.estado_sin_conexion),
            onReintentar,
            "boton_reintentar_vencimiento",
        )
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
