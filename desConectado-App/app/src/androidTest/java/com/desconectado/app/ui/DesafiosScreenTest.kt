package com.desconectado.app.ui

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
        compose.onNodeWithText("Duración: 30 minutos").assertIsDisplayed()
        compose.onNodeWithText("10 puntos").assertIsDisplayed()

        compose.onNodeWithText("Salir a trotar").assertIsDisplayed()
        compose.onNodeWithText("Duración: 45 minutos").assertIsDisplayed()
        compose.onNodeWithText("20 puntos").assertIsDisplayed()
    }

    @Test
    fun existenLasTresDificultadesComoTexto() {
        mostrar(DesafiosUiState.Lista(desafios))

        compose.onNodeWithText("Fácil").assertIsDisplayed()
        compose.onNodeWithText("Normal").assertIsDisplayed()
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
