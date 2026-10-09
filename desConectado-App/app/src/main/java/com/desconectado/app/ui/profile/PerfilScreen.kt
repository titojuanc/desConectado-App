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
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.desconectado.app.domain.model.CosmeticPreferences
import com.desconectado.app.domain.model.DesafioHecho
import com.desconectado.app.domain.model.Perfil
import com.desconectado.app.domain.model.RedeemedReward
import com.desconectado.app.domain.model.TipoRecompensa
import com.desconectado.app.domain.model.UserPreferences
import com.desconectado.app.ui.components.PantallaCargando
import com.desconectado.app.ui.components.PantallaError
import com.desconectado.app.ui.components.PantallaSinConexion

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
                    onCerrarSesion,
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
    var metaSeleccionada by remember(estado.preferencias.weeklyGoalMinutes) {
        mutableStateOf(estado.preferencias.weeklyGoalMinutes)
    }
    var menuAbierto by remember { mutableStateOf(false) }
    FilaCuenta(icono = Icons.Filled.Flag, titulo = stringResource(R.string.perfil_meta_semanal)) {
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
    }
    Row(modifier = Modifier.fillMaxWidth().padding(end = 16.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
        Button(
            onClick = { metaSeleccionada?.let(onGuardarMeta) },
            enabled = !estado.guardando && metaSeleccionada != null && metaSeleccionada != estado.preferencias.weeklyGoalMinutes,
            modifier = Modifier.testTag("boton_guardar_meta"),
        ) {
            Text(stringResource(R.string.perfil_meta_guardar))
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
