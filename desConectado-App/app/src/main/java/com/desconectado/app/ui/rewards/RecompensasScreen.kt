package com.desconectado.app.ui.rewards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.desconectado.app.domain.model.AchievementProgress
import com.desconectado.app.domain.model.AchievementCriterion
import com.desconectado.app.domain.model.CategoriaDesafio
import com.desconectado.app.ui.points.SaldoUiState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.Tab
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import com.desconectado.app.ui.profile.ProgresoUiState
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.desconectado.app.R
import com.desconectado.app.domain.model.Recompensa
import com.desconectado.app.domain.model.PendingRedemption
import com.desconectado.app.domain.model.CosmeticOwnership
import com.desconectado.app.domain.model.CosmeticPreferences
import com.desconectado.app.domain.model.TipoRecompensa
import com.desconectado.app.domain.esCosmetico
import com.desconectado.app.ui.components.PantallaCargando
import com.desconectado.app.ui.components.PantallaError
import com.desconectado.app.ui.components.PantallaSinConexion
import kotlinx.coroutines.flow.Flow

@Composable
fun RecompensasScreen(
    estado: RecompensasUiState,
    onReintentar: () -> Unit,
    onCanjear: ((Recompensa) -> Unit)? = null,
    feedbackEvents: Flow<CanjeFeedback>? = null,
    canjePendiente: PendingRedemption? = null,
    propiedad: List<CosmeticOwnership> = emptyList(),
    preferencias: CosmeticPreferences = CosmeticPreferences(),
    onAplicarCosmetico: ((String) -> Unit)? = null,
    onQuitarCosmetico: ((TipoRecompensa) -> Unit)? = null,
    logros: ProgresoUiState = ProgresoUiState.Oculto,
    onReintentarLogros: () -> Unit = {},
    saldo: SaldoUiState = SaldoUiState.Cargando,
    onCanjeExitoso: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val snackbar = remember { SnackbarHostState() }
    val mensajeInsuficiente = stringResource(R.string.canje_saldo_insuficiente)
    val mensajeSinPremios = stringResource(R.string.canje_caja_sin_premios)
    val mensajeExito = stringResource(R.string.canje_exitoso)
    val mensajeSinConexion = stringResource(R.string.desafio_requiere_conexion)
    val mensajeReglas = stringResource(R.string.canje_reglas_denegadas)
    val mensajeError = stringResource(R.string.canje_error)
    val refrescarSaldo by rememberUpdatedState(onCanjeExitoso)
    LaunchedEffect(feedbackEvents) {
        feedbackEvents?.collect { evento ->
            if (evento == CanjeFeedback.Exitoso) refrescarSaldo()
            val mensaje = when (evento) {
                CanjeFeedback.SaldoInsuficiente -> mensajeInsuficiente
                CanjeFeedback.RecompensaNoDisponible -> mensajeSinPremios
                CanjeFeedback.Exitoso -> mensajeExito
                CanjeFeedback.SinConexion -> mensajeSinConexion
                CanjeFeedback.FirestoreNoAutorizado -> mensajeReglas
                CanjeFeedback.Error -> mensajeError
            }
            snackbar.showSnackbar(mensaje, duration = SnackbarDuration.Short)
        }
    }
    Box(modifier = modifier.fillMaxSize()) {
        var seccion by rememberSaveable { mutableStateOf(0) }
        Column {
            Text(
                stringResource(if (seccion == 0) R.string.recompensas_subtitulo_tienda else R.string.recompensas_subtitulo_logros),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Row(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
                .clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant).padding(3.dp)) {
                Tab(selected = seccion == 0, onClick = { seccion = 0 },
                    text = { Text(stringResource(R.string.recompensas_canjeables)) },
                    selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(50))
                        .background(if (seccion == 0) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent)
                        .testTag("seccion_canjeables"))
                Tab(selected = seccion == 1, onClick = { seccion = 1 },
                    text = { Text(stringResource(R.string.perfil_logros_titulo)) },
                    selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(50))
                        .background(if (seccion == 1) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent)
                        .testTag("seccion_logros"))
            }
            if (seccion == 1) {
                ListaLogros(logros, onReintentarLogros)
            } else {
                when (estado) {
                    RecompensasUiState.Cargando -> PantallaCargando(Modifier.fillMaxSize())
                    RecompensasUiState.Error -> PantallaError(onReintentar, Modifier.fillMaxSize())
                    RecompensasUiState.SinConexion -> PantallaSinConexion(onReintentar, Modifier.fillMaxSize())
                    is RecompensasUiState.Lista -> ListaRecompensas(
                        estado.recompensas,
                        onCanjear,
                        canjePendiente,
                        propiedad,
                        preferencias,
                        onAplicarCosmetico,
                        onQuitarCosmetico,
                        saldo,
                        Modifier.fillMaxSize(),
                    )
                }
            }
        }
        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ListaLogros(estado: ProgresoUiState, onReintentar: () -> Unit) {
    when (estado) {
        ProgresoUiState.Cargando -> PantallaCargando(Modifier.fillMaxSize())
        ProgresoUiState.Error -> PantallaError(onReintentar, Modifier.fillMaxSize())
        ProgresoUiState.SinConexion -> PantallaSinConexion(onReintentar, Modifier.fillMaxSize())
        ProgresoUiState.Oculto -> Text(stringResource(R.string.perfil_logros_vacios), modifier = Modifier.padding(16.dp))
        is ProgresoUiState.Datos -> LazyVerticalGrid(
            columns = GridCells.Adaptive(120.dp),
            modifier = Modifier.fillMaxSize().testTag("lista_logros"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).testTag("resumen_logros_tienda")) {
                    Image(painterResource(R.drawable.recompensa_amanecer), contentDescription = null,
                        contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                    Column(modifier = Modifier.background(Color.Black.copy(alpha = 0.65f)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.recompensas_tus_logros), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(stringResource(R.string.recompensas_logros_resumen, estado.logros.count { it.unlocked }, estado.logros.size), color = Color.White)
                        LinearProgressIndicator(progress = { if (estado.logros.isEmpty()) 0f else estado.logros.count { it.unlocked }.toFloat() / estado.logros.size }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            if (estado.logros.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { Text(stringResource(R.string.perfil_logros_vacios)) }
            }
            items(estado.logros, key = { it.definition.id }) { logro ->
                TarjetaLogro(logro)
            }
        }
    }
}

@Composable
private fun TarjetaLogro(logro: AchievementProgress) {
    val color = when (logro.definition.category) {
        CategoriaDesafio.ENFOCARME -> Color(0xFFB9B0DF)
        CategoriaDesafio.SOCIALIZAR -> Color(0xFFEAA5AA)
        CategoriaDesafio.MOVERME -> Color(0xFF8FC5DE)
        else -> if (logro.definition.criterion == AchievementCriterion.COMPLETED_SECONDS) Color(0xFFE9C46A) else MaterialTheme.colorScheme.primary
    }
    val icon = when {
        logro.definition.category == CategoriaDesafio.ENFOCARME -> Icons.Filled.Psychology
        logro.definition.category == CategoriaDesafio.SOCIALIZAR -> Icons.Filled.Groups
        logro.definition.category == CategoriaDesafio.MOVERME -> Icons.Filled.Landscape
        logro.definition.category == CategoriaDesafio.DESCANSAR -> Icons.Filled.Spa
        logro.definition.criterion == AchievementCriterion.EXPLORED_CATEGORIES -> Icons.Filled.Explore
        logro.definition.criterion == AchievementCriterion.SINGLE_CHALLENGE_SECONDS -> Icons.Filled.WbSunny
        logro.definition.criterion == AchievementCriterion.COMPLETED_SECONDS -> Icons.Filled.Star
        logro.threshold == 1 -> Icons.Filled.Eco
        else -> Icons.AutoMirrored.Filled.DirectionsWalk
    }
    Card(modifier = Modifier.fillMaxWidth().testTag("logro_recompensas_${logro.definition.id}"),
        shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(color.copy(alpha = 0.14f)).border(2.dp, color.copy(alpha = 0.4f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            }
            Text(logro.definition.name, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(logro.definition.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            if (logro.unlocked) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Text(stringResource(R.string.perfil_logro_desbloqueado), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                }
            } else {
                LinearProgressIndicator(progress = { (logro.progress.toFloat() / logro.threshold.coerceAtLeast(1)).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().testTag("progreso_logro_${logro.definition.id}"))
                Text(stringResource(R.string.perfil_logro_progreso, logro.progress, logro.threshold), style = MaterialTheme.typography.labelSmall)
                if (logro.progress == 0) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                        Text(stringResource(R.string.perfil_logro_aun_no), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ListaRecompensas(
    recompensas: List<Recompensa>,
    onCanjear: ((Recompensa) -> Unit)?,
    canjePendiente: PendingRedemption?,
    propiedad: List<CosmeticOwnership>,
    preferencias: CosmeticPreferences,
    onAplicarCosmetico: ((String) -> Unit)?,
    onQuitarCosmetico: ((TipoRecompensa) -> Unit)?,
    saldo: SaldoUiState,
    modifier: Modifier = Modifier,
) {
    val ownedIds = propiedad.mapTo(mutableSetOf()) { it.rewardId }
    val hayPremiosParaCaja = recompensas.any { it.kind.esCosmetico() && it.id !in ownedIds }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(156.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("lista_recompensas"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = stringResource(R.string.recompensas_aclaracion),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (canjePendiente != null) {
            item(span = { GridItemSpan(maxLineSpan) }) { CanjePendiente(canjePendiente) }
        }
        val orden = listOf(TipoRecompensa.TEMA, TipoRecompensa.FONDO_ENFOQUE) + TipoRecompensa.entries.filter { it != TipoRecompensa.TEMA && it != TipoRecompensa.FONDO_ENFOQUE }
        orden.forEach { kind ->
            val grupo = recompensas.filter { it.kind == kind }
            if (grupo.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        Icon(when (kind) { TipoRecompensa.TEMA -> Icons.Filled.Palette; TipoRecompensa.FONDO_ENFOQUE -> Icons.Filled.Wallpaper; else -> Icons.Filled.Redeem }, contentDescription = null)
                        Text(stringResource(tituloGrupo(kind)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
                items(grupo, key = { it.id }) { recompensa ->
                    TarjetaRecompensa(recompensa, onCanjear, canjePendiente != null, recompensa.id in ownedIds, hayPremiosParaCaja,
                        preferencias.activeCosmetics[recompensa.kind] == recompensa.id, saldo)
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(text = stringResource(R.string.recompensas_mis_cosmeticos), style = MaterialTheme.typography.titleMedium)
            if (propiedad.isEmpty()) {
                Text(text = stringResource(R.string.recompensas_sin_cosmeticos), modifier = Modifier.testTag("cosmeticos_vacios"))
            }
        }
        items(propiedad, key = { it.rewardId }) { cosmetico ->
            TarjetaCosmetico(cosmetico, preferencias, onAplicarCosmetico, onQuitarCosmetico)
        }
    }
}

@Composable
private fun CanjePendiente(pendiente: PendingRedemption) {
    Card(modifier = Modifier.fillMaxWidth().testTag("canje_pendiente")) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = stringResource(R.string.canje_pendiente_titulo), style = MaterialTheme.typography.titleMedium)
            Text(text = pendiente.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(
                    R.string.canje_pendiente_detalle,
                    pendiente.pointsDebited,
                    pendiente.costPoints,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun TarjetaRecompensa(
    recompensa: Recompensa,
    onCanjear: ((Recompensa) -> Unit)?,
    canjePendiente: Boolean,
    poseida: Boolean,
    hayPremiosParaCaja: Boolean,
    activa: Boolean,
    saldo: SaldoUiState,
) {
    val available = (saldo as? SaldoUiState.Disponible)?.puntos
    Card(modifier = Modifier.fillMaxWidth().testTag("recompensa_${recompensa.id}"), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Box {
            MiniaturaRecompensa(recompensa.id, recompensa.kind, recompensa.config,
                Modifier.testTag("imagen_recompensa_${recompensa.id}"))
            if (activa) {
                Text(stringResource(R.string.recompensas_en_uso), color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp).clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary).padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = recompensa.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = recompensa.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text(text = recompensa.costPoints.toString(), style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.testTag("costo_recompensa_${recompensa.id}"))
            }
            if (poseida) {
                Text(text = stringResource(if (activa) R.string.recompensas_en_uso else R.string.recompensas_ya_poseida), color = MaterialTheme.colorScheme.primary)
            } else if (onCanjear != null) {
                if (available == null) Text(stringResource(R.string.recompensas_saldo_pendiente), style = MaterialTheme.typography.labelSmall)
                if (available != null && available < recompensa.costPoints) {
                    LinearProgressIndicator(progress = { (available.toFloat() / recompensa.costPoints.coerceAtLeast(1)).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                    Text(stringResource(R.string.recompensas_puntos_faltantes, recompensa.costPoints - available), style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.testTag("saldo_insuficiente_${recompensa.id}"))
                }
                Button(
                    onClick = { onCanjear(recompensa) },
                    enabled = !canjePendiente && available != null && available >= recompensa.costPoints && (recompensa.kind != TipoRecompensa.CAJA_SORPRESA || hayPremiosParaCaja),
                    modifier = Modifier.fillMaxWidth().testTag("boton_canjear_${recompensa.id}"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    Text(if (recompensa.kind == TipoRecompensa.CAJA_SORPRESA && !hayPremiosParaCaja) {
                        stringResource(R.string.recompensas_caja_agotada)
                    } else {
                        stringResource(R.string.canje_confirmar_accion)
                    })
                }
            }
        }
    }
}

private fun ilustracionRecompensa(id: String, kind: TipoRecompensa, config: Map<String, String>): Int = when {
    config["palette"] == "sunset" || id.contains("atardecer") -> R.drawable.recompensa_atardecer
    config["palette"] == "ocean" || id.contains("oceano") -> R.drawable.recompensa_oceano
    id.contains("noche") || config["palette"] == "night" -> R.drawable.recompensa_noche
    id.contains("amanecer") -> R.drawable.recompensa_amanecer
    kind == TipoRecompensa.FONDO_ENFOQUE -> R.drawable.recompensa_montanas
    else -> R.drawable.recompensa_bosque
}

@Composable
private fun MiniaturaRecompensa(id: String, kind: TipoRecompensa, config: Map<String, String>, modifier: Modifier = Modifier) {
    val frame = modifier.fillMaxWidth().aspectRatio(1.85f)
    if (kind == TipoRecompensa.TEMA || kind == TipoRecompensa.FONDO_ENFOQUE) {
        Image(painterResource(ilustracionRecompensa(id, kind, config)), contentDescription = null,
            contentScale = ContentScale.Crop, modifier = frame)
    } else {
        val icon = when (kind) {
            TipoRecompensa.PACK_ICONOS -> Icons.Filled.Widgets
            TipoRecompensa.MARCO_PERFIL -> Icons.Filled.Person
            TipoRecompensa.ICONO_PUNTOS -> Icons.Filled.Star
            TipoRecompensa.SONIDO_COMPLETADO -> Icons.Filled.MusicNote
            TipoRecompensa.ANIMACION_COMPLETADO -> Icons.Filled.AutoAwesome
            TipoRecompensa.INSIGNIA -> Icons.Filled.WorkspacePremium
            else -> Icons.Filled.Redeem
        }
        Box(modifier = frame.background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
        }
    }
}

private fun tituloGrupo(kind: TipoRecompensa): Int = when (kind) {
    TipoRecompensa.TEMA -> R.string.recompensas_grupo_temas
    TipoRecompensa.FONDO_ENFOQUE -> R.string.recompensas_grupo_fondos
    TipoRecompensa.PACK_ICONOS -> R.string.recompensas_grupo_iconos
    TipoRecompensa.MARCO_PERFIL -> R.string.recompensas_grupo_marcos
    TipoRecompensa.ICONO_PUNTOS -> R.string.recompensas_grupo_puntos
    TipoRecompensa.ANIMACION_COMPLETADO -> R.string.recompensas_grupo_animaciones
    TipoRecompensa.SONIDO_COMPLETADO -> R.string.recompensas_grupo_sonidos
    TipoRecompensa.CAJA_SORPRESA -> R.string.recompensas_grupo_cajas
    TipoRecompensa.CUPON -> R.string.recompensas_grupo_cupones
    TipoRecompensa.INSIGNIA -> R.string.perfil_logros_titulo
}

@Composable
private fun TarjetaCosmetico(
    cosmetico: CosmeticOwnership,
    preferencias: CosmeticPreferences,
    onAplicar: ((String) -> Unit)?,
    onQuitar: ((TipoRecompensa) -> Unit)?,
) {
    val activo = preferencias.activeCosmetics[cosmetico.kind] == cosmetico.rewardId
    Card(modifier = Modifier.fillMaxWidth().testTag("cosmetico_${cosmetico.rewardId}"), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        MiniaturaRecompensa(cosmetico.rewardId, cosmetico.kind, cosmetico.config)
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = cosmetico.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = stringResource(if (activo) R.string.recompensas_en_uso else tituloGrupo(cosmetico.kind)), style = MaterialTheme.typography.bodySmall,
                color = if (activo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            if (onAplicar != null && onQuitar != null) {
                Button(
                    onClick = { if (activo) onQuitar(cosmetico.kind) else onAplicar(cosmetico.rewardId) },
                    modifier = Modifier.fillMaxWidth().testTag("boton_${if (activo) "quitar" else "aplicar"}_${cosmetico.rewardId}"),
                ) {
                    Text(stringResource(if (activo) R.string.recompensas_quitar else R.string.recompensas_aplicar))
                }
            }
        }
    }
}
