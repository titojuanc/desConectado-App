package com.desconectado.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.desconectado.app.domain.model.Conectividad
import com.desconectado.app.ui.navigation.MainShell
import com.desconectado.app.ui.points.SaldoUiState
import com.desconectado.app.ui.theme.DesConectadoTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Indicador de puntos de la navegación principal (FR-028, FR-033), sin Firebase. */
@RunWith(AndroidJUnit4::class)
class PuntosUiTest {

    @get:Rule
    val compose = createComposeRule()

    private fun contenido(etiqueta: String): @androidx.compose.runtime.Composable () -> Unit = {
        Box { Text(text = etiqueta, modifier = Modifier.testTag(etiqueta)) }
    }

    private fun mostrar(saldo: SaldoUiState, onDestinoCambiado: () -> Unit = {}) = compose.setContent {
        DesConectadoTheme {
            MainShell(
                conectividad = Conectividad.CONECTADO,
                saldo = saldo,
                onDestinoCambiado = onDestinoCambiado,
                desafios = contenido("contenido_desafios"),
                recompensas = contenido("contenido_recompensas"),
                perfil = contenido("contenido_perfil"),
            )
        }
    }

    @Test
    fun elIndicadorEstaVisibleEnLasTresPestanasConElSaldoFormateado() {
        mostrar(SaldoUiState.Disponible(1250))

        for (pestana in listOf("tab_desafios", "tab_recompensas", "tab_perfil")) {
            compose.onNodeWithTag(pestana).performClick()
            compose.onNodeWithTag("indicador_puntos").assertIsDisplayed().assertTextEquals("1.250")
        }
    }

    @Test
    fun unaCuentaNuevaMuestraCero() {
        mostrar(SaldoUiState.Disponible(0))

        compose.onNodeWithTag("indicador_puntos").assertIsDisplayed().assertTextEquals("0")
    }

    @Test
    fun mientrasCargaMuestraUnGuionYNoUnCero() {
        mostrar(SaldoUiState.Cargando)

        compose.onNodeWithTag("indicador_puntos").assertIsDisplayed().assertTextEquals("–")
    }

    @Test
    fun sinSaldoDisponibleMuestraUnGuionYNuncaUnCero() {
        mostrar(SaldoUiState.NoDisponible)

        compose.onNodeWithTag("indicador_puntos").assertIsDisplayed().assertTextEquals("–")
    }

    @Test
    fun tocarElIndicadorDesdeDesafiosORecompensasAbreElPerfil() {
        mostrar(SaldoUiState.Disponible(10))
        compose.onNodeWithTag("contenido_desafios").assertIsDisplayed()

        compose.onNodeWithTag("indicador_puntos").performClick()
        compose.onNodeWithTag("contenido_perfil").assertIsDisplayed()

        compose.onNodeWithTag("tab_recompensas").performClick()
        compose.onNodeWithTag("contenido_recompensas").assertIsDisplayed()
        compose.onNodeWithTag("indicador_puntos").performClick()
        compose.onNodeWithTag("contenido_perfil").assertIsDisplayed()
    }

    @Test
    fun tocarElIndicadorDesdeElPerfilNoCambiaDePantalla() {
        mostrar(SaldoUiState.Disponible(10))
        compose.onNodeWithTag("tab_perfil").performClick()

        compose.onNodeWithTag("indicador_puntos").performClick()

        compose.onNodeWithTag("contenido_perfil").assertIsDisplayed()
    }

    @Test
    fun alCambiarDePestanaSeAvisaParaRecargarElSaldo() {
        var avisos = 0
        mostrar(SaldoUiState.Disponible(10), onDestinoCambiado = { avisos++ })
        compose.waitForIdle()
        val alInicio = avisos

        compose.onNodeWithTag("tab_recompensas").performClick()
        compose.onNodeWithTag("tab_perfil").performClick()
        compose.waitForIdle()

        assertTrue("Se esperaban 2 avisos más ($alInicio -> $avisos)", avisos >= alInicio + 2)
    }
}
