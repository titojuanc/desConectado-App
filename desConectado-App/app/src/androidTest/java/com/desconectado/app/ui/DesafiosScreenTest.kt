package com.desconectado.app.ui

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import com.desconectado.app.ui.points.SaldoUiState
import java.io.File
import org.junit.Assert.assertTrue
import com.desconectado.app.R
import com.desconectado.app.domain.model.Conectividad
import com.desconectado.app.domain.model.Desafio
import com.desconectado.app.domain.model.CategoriaDesafio
import com.desconectado.app.domain.model.Dificultad
import com.desconectado.app.ui.challenges.DesafiosScreen
import com.desconectado.app.ui.challenges.DesafiosUiState
import com.desconectado.app.ui.navigation.MainShell
import com.desconectado.app.ui.theme.DesConectadoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DesafiosScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val desafios = listOf(
        Desafio("d1", "Salir a caminar", "Tomá aire y mirá a tu alrededor.", 30, Dificultad.FACIL, 10, 1, category = CategoriaDesafio.MOVERME),
        Desafio("d2", "Salir a trotar", "Poné el cuerpo en movimiento.", 45, Dificultad.NORMAL, 20, 2, category = CategoriaDesafio.MOVERME),
        Desafio("d3", "Leer un rato", "Dale toda tu atención al libro.", 25, Dificultad.FACIL, 10, 3, category = CategoriaDesafio.ENFOCARME),
        Desafio("d4", "Descansar al aire libre", "Una pausa sin redes sociales.", 60, Dificultad.DIFICIL, 40, 4, category = CategoriaDesafio.DESCANSAR),
    )

    private fun texto(id: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id)

    private fun mostrar(estado: DesafiosUiState, onReintentar: () -> Unit = {}) = compose.setContent {
        DesConectadoTheme { DesafiosScreen(estado = estado, onReintentar = onReintentar) }
    }

    @Test
    fun cadaTarjeta_muestraTituloDescripcionDuracionPuntosYDificultad() {
        mostrar(DesafiosUiState.Lista(desafios))

        compose.onNodeWithTag("lista_desafios").assertIsDisplayed()
        compose.onNodeWithText("Salir a caminar").assertIsDisplayed()
        compose.onNodeWithText("Tomá aire y mirá a tu alrededor.").assertIsDisplayed()
        compose.onNodeWithTag("duracion_desafio_d1").assertTextEquals("30 minutos")
        compose.onNodeWithTag("puntos_desafio_d1", useUnmergedTree = true).assertTextEquals("10")
        compose.onNodeWithTag("ilustracion_desafio_d1").assertIsDisplayed()

        compose.onNodeWithTag("lista_desafios").performScrollToIndex(1)
        compose.onNodeWithText("Salir a trotar").assertIsDisplayed()
        compose.onNodeWithTag("duracion_desafio_d2").assertTextEquals("45 minutos")
        compose.onNodeWithTag("puntos_desafio_d2", useUnmergedTree = true).assertTextEquals("20")
    }

    @Test
    fun existenLasTresDificultadesComoTexto() {
        mostrar(DesafiosUiState.Lista(desafios))

        compose.onAllNodesWithText("Fácil")[0].assertIsDisplayed()
        compose.onNodeWithTag("lista_desafios").performScrollToIndex(1)
        compose.onNodeWithText("Normal").assertIsDisplayed()
        compose.onNodeWithTag("lista_desafios").performScrollToIndex(3)
        compose.onNodeWithText("Difícil").assertIsDisplayed()
    }

    @Test
    fun sinAccionDeInicio_noAparecenBotonesEnLasTarjetas() {
        mostrar(DesafiosUiState.Lista(desafios))

        compose.onNodeWithTag("boton_iniciar_desafio_d1").assertDoesNotExist()
        compose.onNodeWithTag("boton_iniciar_desafio_d2").assertDoesNotExist()
        compose.onNodeWithTag("boton_iniciar_desafio_d3").assertDoesNotExist()
    }

    @Test
    fun capturaCatalogoIlustradoConCabeceraYNavegacion() {
        compose.setContent {
            DesConectadoTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MainShell(
                        conectividad = Conectividad.CONECTADO,
                        saldo = SaldoUiState.Disponible(0),
                        desafios = { DesafiosScreen(DesafiosUiState.Lista(desafios), onReintentar = {}, onIniciar = {}) },
                    )
                }
            }
        }
        compose.onNodeWithTag("marca_app").assertIsDisplayed()
        compose.onNodeWithTag("ilustracion_desafio_d1").assertIsDisplayed()
        compose.onNodeWithTag("boton_iniciar_desafio_d1").assertExists()
        compose.onNodeWithTag("tab_desafios").assertIsDisplayed()
        compose.waitForIdle()
        val screenshot = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        val context = ApplicationProvider.getApplicationContext<Context>()
        val destination = File(context.getExternalFilesDir(null), "desafios-reference.png")
        destination.outputStream().use { output ->
            assertTrue(screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output))
        }
        val copy = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("cp ${destination.absolutePath} /data/local/tmp/desafios-reference.png")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(copy).use { it.readBytes() }
        screenshot.recycle()
    }

    @Test
    fun comenzarEnviaElDesafioDeLaTarjetaYConservaIlustracion() {
        var iniciado: Desafio? = null
        compose.setContent {
            DesConectadoTheme {
                DesafiosScreen(DesafiosUiState.Lista(desafios), onReintentar = {}, onIniciar = { iniciado = it })
            }
        }
        compose.onNodeWithText("¿Cómo querés desconectarte hoy?").assertIsDisplayed()
        compose.onNodeWithTag("ilustracion_desafio_d1").assertIsDisplayed()
        compose.onNodeWithTag("boton_iniciar_desafio_d1").performScrollTo().performClick()
        assertEquals(desafios.first(), iniciado)
    }

    @Test
    fun desafioDePruebaMuestraDiezSegundosYUnPunto() {
        val quick = desafios.first().copy(id = "debug", durationMinutes = 1, durationSeconds = 10, points = 1)
        mostrar(DesafiosUiState.Lista(listOf(quick)))
        compose.onNodeWithTag("duracion_desafio_debug").assertTextEquals("10 segundos")
        compose.onNodeWithTag("puntos_desafio_debug", useUnmergedTree = true).assertTextEquals("1")
    }

    @Test
    fun empiezaEnTodosYMandaLaCategoriaSeleccionada() {
        var seleccion: CategoriaDesafio? = CategoriaDesafio.DESCANSAR
        compose.setContent {
            DesConectadoTheme {
                DesafiosScreen(
                    estado = DesafiosUiState.Lista(desafios),
                    onReintentar = {},
                    onCategoriaSeleccionada = { seleccion = it },
                )
            }
        }

        compose.onNodeWithTag("filtro_todos").assertIsDisplayed().performClick()
        assertEquals(null, seleccion)
        compose.onNodeWithTag("filtro_move").performClick()
        assertEquals(CategoriaDesafio.MOVERME, seleccion)
    }

    @Test
    fun elEstadoDeError_muestraReintentarYLoInvoca() {
        var reintentos = 0
        mostrar(DesafiosUiState.Error, onReintentar = { reintentos++ })

        compose.onNodeWithText(texto(R.string.estado_error)).assertIsDisplayed()
        compose.onNodeWithTag("boton_reintentar").assertIsDisplayed().performClick()

        assertEquals(1, reintentos)
    }

    @Test
    fun elEstadoSinConexion_explicaQueSeRequiereConexionYOfreceReintentar() {
        mostrar(DesafiosUiState.SinConexion)

        compose.onNodeWithText(texto(R.string.estado_sin_conexion)).assertIsDisplayed()
        compose.onNodeWithTag("boton_reintentar").assertIsDisplayed()
    }

    @Test
    fun sinConexion_laNavegacionMuestraElAvisoArribaDeLaLista() {
        compose.setContent {
            DesConectadoTheme {
                MainShell(
                    conectividad = Conectividad.SIN_CONEXION,
                    desafios = {
                        DesafiosScreen(estado = DesafiosUiState.Lista(desafios), onReintentar = {})
                    },
                )
            }
        }

        compose.onNodeWithTag("aviso_sin_conexion").assertIsDisplayed()
        compose.onNodeWithTag("lista_desafios").assertIsDisplayed()
    }
}
