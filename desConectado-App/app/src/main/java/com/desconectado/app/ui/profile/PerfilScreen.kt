package com.desconectado.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.desconectado.app.R
import com.desconectado.app.domain.ProgressMetrics
import com.desconectado.app.domain.formatearDuracion
import com.desconectado.app.domain.formatearFechaDesafio
import com.desconectado.app.domain.formatearPuntos
import com.desconectado.app.domain.model.AchievementProgress
import com.desconectado.app.domain.model.CosmeticOwnership
import com.desconectado.app.domain.model.CosmeticPreferences
import com.desconectado.app.domain.model.DesafioHecho
import com.desconectado.app.domain.model.Perfil
import com.desconectado.app.domain.model.RedeemedReward
import com.desconectado.app.domain.model.TipoRecompensa
import com.desconectado.app.domain.model.UserPreferences
import com.desconectado.app.ui.components.PantallaCargando
import com.desconectado.app.ui.components.PantallaError
import com.desconectado.app.ui.components.PantallaSinConexion
import com.desconectado.app.ui.auth.SelectorMetaDiaria
import com.desconectado.app.ui.rewards.MiniaturaRecompensa
import com.desconectado.app.ui.rewards.tituloGrupo

/**
 * Perfil con el diseño de referencia: tarjeta de cabecera con avatar y edición, grilla "Tu
 * progreso", tarjeta "Esta semana" con la meta, medallas horizontales y la lista "Cuenta y
 * preferencias". Sin contraseña (FR-010, FR-021); los historiales fallan por separado y cerrar
 * sesión está disponible en todos los estados.
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
    inventarioCosmeticos: List<CosmeticOwnership> = emptyList(),
    onAplicarCosmetico: (String) -> Unit = {},
    onQuitarCosmetico: (TipoRecompensa) -> Unit = {},
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
    mostrarControlDebug: Boolean = false,
    debugHabilitado: Boolean = false,
    onDebugHabilitadoChange: (Boolean) -> Unit = {},
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
                    inventarioCosmeticos,
                    onAplicarCosmetico,
                    onQuitarCosmetico,
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
                    onCerrarSesion,
                    mostrarControlDebug,
                    debugHabilitado,
                    onDebugHabilitadoChange,
                )
            }
        }
        // Sin datos no hay scroll: el botón queda fijo para poder salir igual.
        if (estado !is PerfilUiState.Datos) {
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
    inventarioCosmeticos: List<CosmeticOwnership>,
    onAplicarCosmetico: (String) -> Unit,
    onQuitarCosmetico: (TipoRecompensa) -> Unit,
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
    onCerrarSesion: () -> Unit,
    mostrarControlDebug: Boolean,
    debugHabilitado: Boolean,
    onDebugHabilitadoChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TarjetaCabecera(
            perfil = perfil,
            preferencias = preferenciasCosmeticas,
            edicion = edicionNombre,
            onEditar = onEditarNombre,
            onCambiar = onCambiarNombre,
            onGuardar = onGuardarNombre,
            onCancelar = onCancelarEdicionNombre,
        )
        ProximoVencimiento(proximoVencimiento, onReintentarVencimiento)
        Progreso(progreso, onReintentarProgreso, (preferenciasUsuario as? PreferenciasPerfilUiState.Datos)?.preferencias?.weeklyGoalMinutes)
        InventarioPerfil(
            canjes = canjes,
            propiedad = inventarioCosmeticos,
            preferencias = preferenciasCosmeticas,
            onReintentar = onReintentarCanjes,
            onAplicar = onAplicarCosmetico,
            onQuitar = onQuitarCosmetico,
        )
        CuentaYPreferencias(
            email = perfil.email,
            preferencias = preferenciasUsuario,
            onReintentar = onReintentarPreferencias,
            onGuardarMeta = onGuardarMetaSemanal,
            onConfigurarNotificaciones = onConfigurarNotificaciones,
            onAbrirAjustesPrivacidad = onAbrirAjustesPrivacidad,
        )
        DesafiosHechos(desafiosHechos, onReintentarPuntos)
        CanjesRealizados(canjes, onReintentarCanjes)
        OutlinedButton(
            onClick = onCerrarSesion,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("boton_cerrar_sesion"),
        ) {
            Text(stringResource(R.string.perfil_cerrar_sesion))
        }
        if (mostrarControlDebug) {
            Row(
                modifier = Modifier.fillMaxWidth().testTag("fila_seccion_debug"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(stringResource(R.string.debug_seccion_switch_titulo), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.debug_seccion_switch_descripcion),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = debugHabilitado,
                    onCheckedChange = onDebugHabilitadoChange,
                    modifier = Modifier.testTag("switch_seccion_debug"),
                )
            }
        }
    }
}

/** Tarjeta superior verde con avatar, nombre, puntos y el botón Editar perfil. */
@Composable
private fun TarjetaCabecera(
    perfil: Perfil,
    preferencias: CosmeticPreferences,
    edicion: EdicionNombreUiState,
    onEditar: () -> Unit,
    onCambiar: (String) -> Unit,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AvatarPerfil(perfil.username, preferencias)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = perfil.username,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.testTag("texto_username"),
                )
                Text(
                    text = "@" + perfil.email.substringBefore('@'),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (preferencias.activeCosmetics[TipoRecompensa.ICONO_PUNTOS] == "point-icon-estrella-especial") {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp).testTag("icono_puntos_cosmetico"),
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = formatearPuntos(perfil.puntos),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.testTag("puntos_perfil"),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.perfil_etiqueta_puntos).lowercase(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )
            Spacer(Modifier.weight(1f))
            if (!edicion.editando) {
                OutlinedButton(onClick = onEditar, modifier = Modifier.testTag("boton_editar_nombre")) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.perfil_editar_perfil))
                }
            }
        }
        if (edicion.editando) {
            OutlinedTextField(
                value = edicion.texto,
                onValueChange = onCambiar,
                label = { Text(stringResource(R.string.perfil_etiqueta_username)) },
                singleLine = true,
                isError = edicion.error,
                modifier = Modifier.fillMaxWidth().testTag("campo_editar_username"),
            )
            if (edicion.error) Text(stringResource(R.string.perfil_error_nombre), color = MaterialTheme.colorScheme.error)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onGuardar, enabled = !edicion.guardando, modifier = Modifier.testTag("boton_guardar_nombre")) {
                    Text(stringResource(R.string.perfil_guardar_nombre))
                }
                TextButton(onClick = onCancelar, enabled = !edicion.guardando, modifier = Modifier.testTag("boton_cancelar_edicion_nombre")) {
                    Text(stringResource(R.string.perfil_cancelar_edicion_nombre))
                }
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
        null -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.primary
    }
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(if (frameId == null) 2.dp else 3.dp, frameColor, CircleShape)
            .testTag(if (frameId == null) "avatar_perfil" else "avatar_perfil_enmarcado"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = iniciales,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Bloque "Tu progreso": grilla de métricas, tarjeta semanal y medallas, como la referencia. */
@Composable
private fun Progreso(estado: ProgresoUiState, onReintentar: () -> Unit, metaSemanalMinutos: Int?) {
    if (estado == ProgresoUiState.Oculto) return
    Column(
        modifier = Modifier.fillMaxWidth().testTag("resumen_progreso"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TituloSeccion(stringResource(R.string.perfil_progreso_titulo))
        when (estado) {
            ProgresoUiState.Oculto -> Unit
            ProgresoUiState.Cargando -> CircularProgressIndicator(modifier = Modifier.testTag("cargando_progreso"))
            ProgresoUiState.Error -> ErrorPuntos(
                stringResource(R.string.perfil_error_progreso), onReintentar, "boton_reintentar_progreso",
            )
            ProgresoUiState.SinConexion -> ErrorPuntos(
                stringResource(R.string.estado_sin_conexion), onReintentar, "boton_reintentar_progreso",
            )
            is ProgresoUiState.Datos -> {
                GrillaProgreso(estado.metricas, estado.logros)
                TarjetaSemana(estado.metricas, metaSemanalMinutos)
                TusLogros(estado.logros)
            }
        }
    }
}

@Composable
private fun GrillaProgreso(metricas: ProgressMetrics, logros: List<AchievementProgress>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TarjetaStat(
                icono = Icons.Filled.Schedule,
                colorIcono = MaterialTheme.colorScheme.primary,
                valor = formatearDuracion((metricas.tiempoCompletadoSegundos / 60).toInt()),
                etiqueta = stringResource(R.string.perfil_stat_tiempo),
                modifier = Modifier.weight(1f),
            )
            TarjetaStat(
                icono = Icons.Filled.EmojiEvents,
                colorIcono = MaterialTheme.colorScheme.primary,
                valor = metricas.desafiosCompletados.toString(),
                etiqueta = stringResource(R.string.perfil_stat_desafios),
                modifier = Modifier.weight(1f),
                tagValor = "stat_desafios_completados",
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TarjetaStat(
                icono = Icons.Filled.LocalFireDepartment,
                colorIcono = Color(0xFFE8694A),
                valor = stringResource(R.string.perfil_racha_valor, metricas.rachaDias),
                etiqueta = stringResource(R.string.perfil_stat_racha),
                modifier = Modifier.weight(1f),
            )
            TarjetaStat(
                icono = Icons.Filled.Star,
                colorIcono = Color(0xFFE9C46A),
                valor = logros.count { it.unlocked }.toString(),
                etiqueta = stringResource(R.string.perfil_stat_logros),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TarjetaStat(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    colorIcono: Color,
    valor: String,
    etiqueta: String,
    modifier: Modifier = Modifier,
    tagValor: String? = null,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(colorIcono.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icono, contentDescription = null, tint = colorIcono, modifier = Modifier.size(20.dp))
        }
        Column {
            Text(
                text = valor,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = if (tagValor == null) Modifier else Modifier.testTag(tagValor),
            )
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TarjetaSemana(metricas: ProgressMetrics, metaSemanalMinutos: Int?) {
    val semanaMinutos = (metricas.tiempoSemanalSegundos / 60).toInt()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
            .padding(16.dp)
            .testTag("tarjeta_semana"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.perfil_esta_semana),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (metaSemanalMinutos != null) {
            Text(
                text = stringResource(
                    if (metricas.metaDiariaCumplida) R.string.perfil_meta_diaria_cumplida else R.string.perfil_meta_diaria_pendiente,
                    ((metricas.segundosRestantesMetaDiaria ?: 0L) + 59) / 60,
                ),
                modifier = Modifier.testTag("estado_meta_diaria"),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = formatearDuracion(semanaMinutos),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = " / " + formatearDuracion(metaSemanalMinutos),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LinearProgressIndicator(
                progress = { (semanaMinutos.toFloat() / metaSemanalMinutos.coerceAtLeast(1)).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
            )
            val restante = metricas.minutosRestantesMetaSemanal
            Text(
                text = if (restante != null && restante > 0) {
                    stringResource(R.string.perfil_progreso_meta_restante, restante)
                } else {
                    stringResource(R.string.perfil_meta_semanal_caption)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = formatearDuracion(semanaMinutos),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.perfil_progreso_meta_pendiente),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Fila horizontal de medallas: desbloqueadas en verde y pendientes con candado, como la referencia. */
@Composable
private fun TusLogros(logros: List<AchievementProgress>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TituloSeccion(stringResource(R.string.perfil_logros_titulo))
        if (logros.isEmpty()) {
            Text(
                text = stringResource(R.string.perfil_medallas_vacias),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("logros_vacios"),
            )
            return
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("fila_medallas"),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            logros.forEach { logro -> Medalla(logro) }
        }
    }
}

@Composable
private fun Medalla(logro: AchievementProgress) {
    Column(
        modifier = Modifier
            .widthIn(min = 88.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
            .testTag(if (logro.unlocked) "medalla_item_${logro.definition.id}" else "logro_bloqueado_${logro.definition.id}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    if (logro.unlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                )
                .border(
                    2.dp,
                    if (logro.unlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (logro.unlocked) Icons.Filled.WorkspacePremium else Icons.Filled.Lock,
                contentDescription = null,
                tint = if (logro.unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = if (logro.unlocked) Modifier.testTag("medalla_${logro.definition.id}") else Modifier,
            )
        }
        Text(
            text = logro.definition.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
        if (!logro.unlocked) {
            Text(
                text = stringResource(R.string.perfil_logro_aun_no),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Lista "Cuenta y preferencias" en filas con ícono, como la referencia. */
@Composable
private fun CuentaYPreferencias(
    email: String,
    preferencias: PreferenciasPerfilUiState,
    onReintentar: () -> Unit,
    onGuardarMeta: (Int) -> Unit,
    onConfigurarNotificaciones: (Boolean) -> Unit,
    onAbrirAjustesPrivacidad: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TituloSeccion(stringResource(R.string.perfil_cuenta_preferencias))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            FilaCuenta(icono = Icons.Filled.Edit, titulo = stringResource(R.string.perfil_etiqueta_email)) {}
            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 48.dp, end = 16.dp, bottom = 10.dp).testTag("texto_email"),
            )
            if (preferencias != PreferenciasPerfilUiState.Oculto) {
                HorizontalDivider(color = MaterialTheme.colorScheme.background)
                Column(modifier = Modifier.testTag("preferencias_perfil")) {
                    when (preferencias) {
                        PreferenciasPerfilUiState.Oculto -> Unit
                        PreferenciasPerfilUiState.Cargando -> Box(Modifier.padding(16.dp)) {
                            CircularProgressIndicator(modifier = Modifier.testTag("cargando_preferencias"))
                        }
                        PreferenciasPerfilUiState.Error -> Box(Modifier.padding(16.dp)) {
                            ErrorPuntos(stringResource(R.string.perfil_preferencias_error), onReintentar, "boton_reintentar_preferencias")
                        }
                        PreferenciasPerfilUiState.SinConexion -> Box(Modifier.padding(16.dp)) {
                            ErrorPuntos(stringResource(R.string.estado_sin_conexion), onReintentar, "boton_reintentar_preferencias")
                        }
                        is PreferenciasPerfilUiState.Datos -> FilasPreferencias(preferencias, onGuardarMeta, onConfigurarNotificaciones)
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.background)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAbrirAjustesPrivacidad)
                    .padding(16.dp)
                    .testTag("boton_ajustes_privacidad"),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(R.string.perfil_privacidad_titulo),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(R.string.perfil_privacidad_descripcion),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FilasPreferencias(
    estado: PreferenciasPerfilUiState.Datos,
    onGuardarMeta: (Int) -> Unit,
    onConfigurarNotificaciones: (Boolean) -> Unit,
) {
    var metaDiariaSeleccionada by remember(estado.preferencias.weeklyGoalMinutes) {
        mutableStateOf(estado.preferencias.weeklyGoalMinutes?.let(UserPreferences::metaDiariaDesdeSemanal))
    }
    val metaSemanalSeleccionada = metaDiariaSeleccionada?.let(UserPreferences::metaSemanalDesdeDiaria)
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        SelectorMetaDiaria(
            selectedMinutes = metaSemanalSeleccionada,
            error = false,
            onSelect = { metaDiariaSeleccionada = it?.div(7) },
            titleResource = R.string.perfil_meta_diaria_titulo,
            modifier = Modifier.testTag("meta_semanal_selector"),
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
            Button(
                onClick = { metaSemanalSeleccionada?.let(onGuardarMeta) },
                enabled = !estado.guardando && metaSemanalSeleccionada != null &&
                    metaSemanalSeleccionada != estado.preferencias.weeklyGoalMinutes,
                modifier = Modifier.testTag("boton_guardar_meta"),
            ) {
                Text(stringResource(R.string.perfil_meta_guardar))
            }
        }
    }
    if (estado.error) {
        Text(
            text = stringResource(R.string.perfil_preferencias_error),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.background)
    FilaCuenta(icono = Icons.Filled.Notifications, titulo = stringResource(R.string.perfil_notificaciones)) {
        Switch(
            checked = estado.preferencias.notificationsEnabled,
            onCheckedChange = onConfigurarNotificaciones,
            enabled = !estado.guardando,
            modifier = Modifier.testTag("toggle_notificaciones"),
        )
    }
}

@Composable
private fun FilaCuenta(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    contenido: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp),
        )
        Text(text = titulo, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        contenido()
    }
}

@Composable
private fun TituloSeccion(texto: String) {
    Text(text = texto, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("proximo_vencimiento"),
        )
        ProximoVencimientoUiState.SinVencimientos -> Text(
            text = stringResource(R.string.perfil_sin_puntos_por_vencer),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        TituloSeccion(stringResource(R.string.perfil_desafios_hechos_titulo))
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
    ) {
        Text(text = desafio.titulo, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
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
        TituloSeccion(stringResource(R.string.perfil_canjes_titulo))
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
            .testTag("canje_${canje.redemptionId}"),
    ) {
        Text(text = canje.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
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
private fun InventarioPerfil(
    canjes: CanjesPerfilUiState,
    propiedad: List<CosmeticOwnership>,
    preferencias: CosmeticPreferences,
    onReintentar: () -> Unit,
    onAplicar: (String) -> Unit,
    onQuitar: (TipoRecompensa) -> Unit,
) {
    var cuponSeleccionado by remember { mutableStateOf<RedeemedReward?>(null) }
    val cupones = (canjes as? CanjesPerfilUiState.Lista)?.canjes.orEmpty()
        .filter { it.kind == TipoRecompensa.CUPON || it.code != null }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.testTag("inventario")) {
        TituloSeccion(stringResource(R.string.perfil_inventario_titulo))
        if (propiedad.isEmpty() && cupones.isEmpty()) {
            when (canjes) {
                CanjesPerfilUiState.Cargando -> CircularProgressIndicator()
                CanjesPerfilUiState.Error -> ErrorPuntos(
                    stringResource(R.string.perfil_error_canjes),
                    onReintentar,
                    "boton_reintentar_inventario",
                )
                CanjesPerfilUiState.SinConexion -> ErrorPuntos(
                    stringResource(R.string.estado_sin_conexion),
                    onReintentar,
                    "boton_reintentar_inventario",
                )
                is CanjesPerfilUiState.Lista -> Text(
                    stringResource(R.string.perfil_inventario_vacio),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("inventario_vacio"),
                )
            }
        } else if (canjes is CanjesPerfilUiState.Error || canjes is CanjesPerfilUiState.SinConexion) {
            ErrorPuntos(
                stringResource(if (canjes is CanjesPerfilUiState.SinConexion) R.string.estado_sin_conexion else R.string.perfil_error_canjes),
                onReintentar,
                "boton_reintentar_inventario",
            )
        }

        if (propiedad.isNotEmpty()) {
            Text(stringResource(R.string.perfil_inventario_cosmeticos), style = MaterialTheme.typography.titleSmall)
            propiedad.forEach { cosmetico ->
                TarjetaCosmetico(cosmetico, preferencias, onAplicar, onQuitar)
            }
        }
        if (cupones.isNotEmpty()) {
            Text(stringResource(R.string.perfil_inventario_cupones), style = MaterialTheme.typography.titleSmall)
            cupones.forEach { cupon ->
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("inventario_cupon_${cupon.redemptionId}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(cupon.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = { cuponSeleccionado = cupon },
                            enabled = cupon.code != null,
                            modifier = Modifier.fillMaxWidth().testTag("boton_usar_cupon_${cupon.redemptionId}"),
                        ) {
                            Text(stringResource(R.string.perfil_inventario_usar_cupon))
                        }
                    }
                }
            }
        }
    }

    cuponSeleccionado?.let { cupon ->
        AlertDialog(
            onDismissRequest = { cuponSeleccionado = null },
            title = { Text(stringResource(R.string.perfil_inventario_codigo_titulo)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(cupon.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = stringResource(R.string.perfil_canje_codigo, cupon.code.orEmpty()),
                        modifier = Modifier.testTag("codigo_cupon_inventario"),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { cuponSeleccionado = null },
                    modifier = Modifier.testTag("boton_cerrar_cupon"),
                ) {
                    Text(stringResource(R.string.perfil_inventario_cerrar))
                }
            },
        )
    }
}

@Composable
private fun TarjetaCosmetico(
    cosmetico: CosmeticOwnership,
    preferencias: CosmeticPreferences,
    onAplicar: (String) -> Unit,
    onQuitar: (TipoRecompensa) -> Unit,
) {
    val activo = preferencias.activeCosmetics[cosmetico.kind] == cosmetico.rewardId
    Card(
        modifier = Modifier.fillMaxWidth().testTag("cosmetico_${cosmetico.rewardId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        MiniaturaRecompensa(cosmetico.rewardId, cosmetico.kind, cosmetico.config)
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = cosmetico.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(if (activo) R.string.recompensas_en_uso else tituloGrupo(cosmetico.kind)),
                style = MaterialTheme.typography.bodySmall,
                color = if (activo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = { if (activo) onQuitar(cosmetico.kind) else onAplicar(cosmetico.rewardId) },
                modifier = Modifier.fillMaxWidth().testTag("boton_${if (activo) "quitar" else "aplicar"}_${cosmetico.rewardId}"),
            ) {
                Text(stringResource(if (activo) R.string.recompensas_quitar else R.string.recompensas_aplicar))
            }
        }
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
