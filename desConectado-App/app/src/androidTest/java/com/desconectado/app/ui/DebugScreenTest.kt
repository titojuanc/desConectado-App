package com.desconectado.app.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.desconectado.app.domain.model.Conectividad
import com.desconectado.app.ui.navigation.ComprobacionDebug
import com.desconectado.app.ui.navigation.AccionRachaDebug
import com.desconectado.app.ui.navigation.DebugScreen
import com.desconectado.app.ui.navigation.EstadoDiagnosticoDebug
import com.desconectado.app.ui.navigation.MainShell
import com.desconectado.app.ui.points.SaldoUiState
import com.desconectado.app.ui.theme.DesConectadoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DebugScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun botonesDeRachaInvocanLasCuatroAccionesYMuestranResultado() {
        val acciones = mutableListOf<AccionRachaDebug>()
        compose.setContent {
            DesConectadoTheme {
                DebugScreen(
                    uid = "qa", email = null, projectId = "demo-desconectado",
                    usaEmulador = true, conectividad = Conectividad.CONECTADO,
                    permisoUsoConcedido = true, diagnostico = EstadoDiagnosticoDebug.SinEjecutar,
                    onEjecutarDiagnostico = {}, onAbrirTutorialPermisos = {}, onAbrirDesafios = {},
                    onAbrirRecompensas = {}, onAbrirPerfil = {}, onRachaDebug = { acciones.add(it) },
                    resultadoRacha = "Saldo real: 25. Meta diaria pendiente.",
                )
            }
        }
        compose.onNodeWithTag("debug_racha_resultado").performScrollTo().assertIsDisplayed()
        listOf(AccionRachaDebug.ACTIVAR, AccionRachaDebug.SUMAR_PUNTOS,
            AccionRachaDebug.COMPLETAR_META, AccionRachaDebug.REINICIAR).forEach {
            compose.onNodeWithTag("debug_racha_${it.name.lowercase()}").performScrollTo().performClick()
        }
        assertEquals(listOf(AccionRachaDebug.ACTIVAR, AccionRachaDebug.SUMAR_PUNTOS,
            AccionRachaDebug.COMPLETAR_META, AccionRachaDebug.REINICIAR), acciones)
    }

    @Test
    fun cuartaPestanaDebugSoloApareceCuandoSeSolicita() {
        compose.setContent {
            DesConectadoTheme {
                MainShell(
                    conectividad = Conectividad.CONECTADO,
                    saldo = SaldoUiState.Disponible(0),
                    mostrarDebug = true,
                    debug = { _, _, _ -> Text("Panel QA") },
                )
            }
        }

        compose.onNodeWithTag("tab_desafios").assertIsDisplayed()
        compose.onNodeWithTag("tab_recompensas").assertIsDisplayed()
        compose.onNodeWithTag("tab_perfil").assertIsDisplayed()
        compose.onNodeWithTag("tab_debug").assertIsDisplayed().performClick()
        compose.onNodeWithText("Panel QA").assertIsDisplayed()
    }

    @Test
    fun debugNoApareceEnNavegacionNormal() {
        compose.setContent {
            DesConectadoTheme {
                MainShell(conectividad = Conectividad.CONECTADO)
            }
        }

        compose.onNodeWithTag("tab_debug").assertDoesNotExist()
    }

    @Test
    fun desactivarDebugOcultaLaPestanaYVuelveADesafios() {
        var debugHabilitado by mutableStateOf(true)
        compose.setContent {
            DesConectadoTheme {
                MainShell(
                    conectividad = Conectividad.CONECTADO,
                    mostrarDebug = debugHabilitado,
                    desafios = { Text("Catálogo de desafíos") },
                    debug = { _, _, _ -> Text("Panel QA") },
                )
            }
        }

        compose.onNodeWithTag("tab_debug").performClick()
        compose.onNodeWithText("Panel QA").assertIsDisplayed()
        compose.runOnIdle { debugHabilitado = false }
        compose.waitForIdle()

        compose.onNodeWithTag("tab_debug").assertDoesNotExist()
        compose.onNodeWithText("Catálogo de desafíos").assertIsDisplayed()
    }

    @Test
    fun escenariosAbrenFlujosRealesYMuestranDiagnosticos() {
        var desafios = 0
        var recompensas = 0
        var perfil = 0
        var tutorial = 0
        var diagnosticos = 0
        compose.setContent {
            DesConectadoTheme {
                DebugScreen(
                    uid = "qa-user-12345678",
                    email = "qa@example.test",
                    projectId = "demo-desconectado",
                    usaEmulador = true,
                    conectividad = Conectividad.CONECTADO,
                    permisoUsoConcedido = false,
                    diagnostico = EstadoDiagnosticoDebug.Listo(
                        listOf(ComprobacionDebug("Catálogo", "4 desafíos [TEST]", true)),
                    ),
                    onEjecutarDiagnostico = { diagnosticos++ },
                    onAbrirTutorialPermisos = { tutorial++ },
                    onAbrirDesafios = { desafios++ },
                    onAbrirRecompensas = { recompensas++ },
                    onAbrirPerfil = { perfil++ },
                )
            }
        }

        compose.onNodeWithText("Catálogo").assertIsDisplayed()
        compose.onNodeWithTag("debug_ejecutar_diagnostico").performClick()
        compose.onNodeWithTag("debug_desafios_abrir").performScrollTo().performClick()
        compose.onNodeWithTag("debug_puntos_abrir").performScrollTo().performClick()
        compose.onNodeWithTag("debug_logros_abrir").performScrollTo().performClick()
        compose.onNodeWithTag("debug_recompensas_abrir").performScrollTo().performClick()
        compose.onNodeWithTag("debug_perfil_abrir").performScrollTo().performClick()
        compose.onNodeWithTag("debug_permisos_abrir").performScrollTo().performClick()

        assertEquals(1, diagnosticos)
        assertEquals(2, desafios)
        assertEquals(2, recompensas)
        assertEquals(1, perfil)
        assertEquals(1, tutorial)
    }
}