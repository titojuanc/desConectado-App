package com.desconectado.app.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.desconectado.app.R
import com.desconectado.app.domain.model.Conectividad
import com.desconectado.app.ui.components.AvisoSinConexion
import com.desconectado.app.ui.points.IndicadorPuntos
import com.desconectado.app.ui.points.SaldoUiState

private enum class Destino(
    val etiqueta: Int,
    val icono: ImageVector,
    val etiquetaPrueba: String,
) {
    DESAFIOS(R.string.tab_desafios, Icons.Filled.Check, "tab_desafios"),
    RECOMPENSAS(R.string.tab_recompensas, Icons.Filled.Star, "tab_recompensas"),
    PERFIL(R.string.tab_perfil, Icons.Filled.Person, "tab_perfil"),
}

/**
 * Estructura principal con sesión iniciada: barra inferior con Desafíos, Recompensas y Perfil, el
 * indicador de puntos arriba a la izquierda en todas (FR-028) y el aviso de conexión mientras no
 * haya red (FR-011). Tocar el indicador lleva al Perfil; desde el Perfil no hace nada. El contenido
 * de cada destino y el saldo llegan como parámetros para poder probar la estructura sin Firebase;
 * [onDestinoCambiado] avisa cada vez que se muestra una pestaña, para recargar el saldo.
 */
@Composable
fun MainShell(
    conectividad: Conectividad,
    modifier: Modifier = Modifier,
    saldo: SaldoUiState = SaldoUiState.Cargando,
    onDestinoCambiado: () -> Unit = {},
    desafios: @Composable () -> Unit = {},
    recompensas: @Composable () -> Unit = {},
    perfil: @Composable () -> Unit = {},
) {
    var destino by rememberSaveable { mutableStateOf(Destino.DESAFIOS) }
    LaunchedEffect(destino) { onDestinoCambiado() }

    Scaffold(
        modifier = modifier,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IndicadorPuntos(estado = saldo, onClick = { destino = Destino.PERFIL })
            }
        },
        bottomBar = {
            NavigationBar {
                Destino.entries.forEach { item ->
                    NavigationBarItem(
                        selected = destino == item,
                        onClick = { destino = item },
                        icon = { Icon(item.icono, contentDescription = null) },
                        label = { Text(stringResource(item.etiqueta)) },
                        modifier = Modifier.testTag(item.etiquetaPrueba),
                    )
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (conectividad == Conectividad.SIN_CONEXION) AvisoSinConexion()
            when (destino) {
                Destino.DESAFIOS -> desafios()
                Destino.RECOMPENSAS -> recompensas()
                Destino.PERFIL -> perfil()
            }
        }
    }
}
