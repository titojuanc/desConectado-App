package com.desconectado.app.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.desconectado.app.R
import com.desconectado.app.domain.model.Conectividad

data class ComprobacionDebug(
    val titulo: String,
    val detalle: String,
    val correcta: Boolean,
)

sealed interface EstadoDiagnosticoDebug {
    data object SinEjecutar : EstadoDiagnosticoDebug
    data object Cargando : EstadoDiagnosticoDebug
    data class Listo(val comprobaciones: List<ComprobacionDebug>) : EstadoDiagnosticoDebug
}

@Composable
fun DebugScreen(
    uid: String,
    email: String?,
    projectId: String,
    usaEmulador: Boolean,
    conectividad: Conectividad,
    permisoUsoConcedido: Boolean,
    diagnostico: EstadoDiagnosticoDebug,
    onEjecutarDiagnostico: () -> Unit,
    onAbrirTutorialPermisos: () -> Unit,
    onAbrirDesafios: () -> Unit,
    onAbrirRecompensas: () -> Unit,
    onAbrirPerfil: () -> Unit,
    onRachaDebug: (AccionRachaDebug) -> Unit = {},
    resultadoRacha: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.debug_titulo), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                stringResource(
                    R.string.debug_entorno,
                    if (usaEmulador) stringResource(R.string.debug_emulador) else stringResource(R.string.debug_produccion),
                    projectId,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("debug_entorno"),
            )
            Text(
                stringResource(R.string.debug_cuenta, email ?: uid.takeLast(8)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (!usaEmulador) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(
                    stringResource(R.string.debug_aviso_produccion),
                    modifier = Modifier.padding(14.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.debug_estado_titulo), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            InfoDebug(stringResource(R.string.debug_conectividad), stringResource(conectividad.toEtiqueta()))
            InfoDebug(
                stringResource(R.string.debug_permiso_uso),
                stringResource(if (permisoUsoConcedido) R.string.debug_concedido else R.string.debug_pendiente),
            )
            Button(
                onClick = onEjecutarDiagnostico,
                modifier = Modifier.fillMaxWidth().testTag("debug_ejecutar_diagnostico"),
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Text(stringResource(R.string.debug_ejecutar_diagnostico), modifier = Modifier.padding(start = 8.dp))
            }
            when (diagnostico) {
                EstadoDiagnosticoDebug.SinEjecutar -> Text(
                    stringResource(R.string.debug_diagnostico_sin_ejecutar),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                EstadoDiagnosticoDebug.Cargando -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                is EstadoDiagnosticoDebug.Listo -> diagnostico.comprobaciones.forEach { ComprobacionFila(it) }
            }
        }

        HorizontalDivider()
        Text(stringResource(R.string.debug_racha_titulo), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.debug_racha_aviso), color = MaterialTheme.colorScheme.error)
        resultadoRacha?.let { Text(it, modifier = Modifier.testTag("debug_racha_resultado")) }
        listOf(
            AccionRachaDebug.ACTIVAR to R.string.debug_racha_activar,
            AccionRachaDebug.SUMAR_PUNTOS to R.string.debug_racha_sumar,
            AccionRachaDebug.COMPLETAR_META to R.string.debug_racha_completar,
            AccionRachaDebug.REINICIAR to R.string.debug_racha_reiniciar,
        ).forEach { (accion, etiqueta) ->
            OutlinedButton(
                onClick = { onRachaDebug(accion) },
                modifier = Modifier.fillMaxWidth().testTag("debug_racha_${accion.name.lowercase()}"),
            ) {
                Text(stringResource(etiqueta))
            }
        }
        HorizontalDivider()
        Text(stringResource(R.string.debug_escenarios_titulo), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        EscenarioDebug(
            titulo = stringResource(R.string.debug_auth_titulo),
            detalle = stringResource(R.string.debug_auth_detalle),
            accion = stringResource(R.string.debug_abrir_perfil),
            testTag = "debug_auth",
            onClick = onAbrirPerfil,
        )
        EscenarioDebug(
            titulo = stringResource(R.string.debug_desafios_titulo),
            detalle = stringResource(R.string.debug_desafios_detalle),
            accion = stringResource(R.string.debug_abrir_desafios),
            testTag = "debug_desafios",
            onClick = onAbrirDesafios,
        )
        EscenarioDebug(
            titulo = stringResource(R.string.debug_puntos_titulo),
            detalle = stringResource(R.string.debug_puntos_detalle),
            accion = stringResource(R.string.debug_abrir_desafios),
            testTag = "debug_puntos",
            onClick = onAbrirDesafios,
        )
        EscenarioDebug(
            titulo = stringResource(R.string.debug_logros_titulo),
            detalle = stringResource(R.string.debug_logros_detalle),
            accion = stringResource(R.string.debug_abrir_recompensas),
            testTag = "debug_logros",
            onClick = onAbrirRecompensas,
        )
        EscenarioDebug(
            titulo = stringResource(R.string.debug_recompensas_titulo),
            detalle = stringResource(R.string.debug_recompensas_detalle),
            accion = stringResource(R.string.debug_abrir_recompensas),
            testTag = "debug_recompensas",
            onClick = onAbrirRecompensas,
        )
        EscenarioDebug(
            titulo = stringResource(R.string.debug_perfil_titulo),
            detalle = stringResource(R.string.debug_perfil_detalle),
            accion = stringResource(R.string.debug_abrir_perfil),
            testTag = "debug_perfil",
            onClick = onAbrirPerfil,
        )
        EscenarioDebug(
            titulo = stringResource(R.string.debug_permisos_titulo),
            detalle = stringResource(R.string.debug_permisos_detalle),
            accion = stringResource(R.string.debug_abrir_tutorial),
            testTag = "debug_permisos",
            onClick = onAbrirTutorialPermisos,
        )
        EscenarioDebug(
            titulo = stringResource(R.string.debug_conexion_titulo),
            detalle = stringResource(R.string.debug_conexion_detalle),
            accion = stringResource(R.string.debug_ejecutar_diagnostico),
            testTag = "debug_conexion",
            onClick = onEjecutarDiagnostico,
        )
        Text(
            stringResource(R.string.debug_nota_solo_lectura),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InfoDebug(titulo: String, valor: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(titulo, style = MaterialTheme.typography.bodyMedium)
        Text(valor, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ComprobacionFila(comprobacion: ComprobacionDebug) {
    Row(
        modifier = Modifier.fillMaxWidth().testTag("debug_check_${comprobacion.titulo}"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = if (comprobacion.correcta) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = if (comprobacion.correcta) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(comprobacion.titulo, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(comprobacion.detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EscenarioDebug(
    titulo: String,
    detalle: String,
    accion: String,
    testTag: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = onClick, modifier = Modifier.testTag("${testTag}_abrir")) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text(accion, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

private fun Conectividad.toEtiqueta(): Int = when (this) {
    Conectividad.CONECTADO -> R.string.debug_conectado
    Conectividad.SIN_CONEXION -> R.string.debug_sin_conexion
}