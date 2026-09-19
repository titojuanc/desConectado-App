package com.desconectado.app.ui

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.desconectado.app.R
import com.desconectado.app.domain.model.Conectividad
import com.desconectado.app.domain.model.Desafio
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
        Desafio("d1", "No uses redes sociales por 30 minutos", "Media hora sin redes.", 30, Dificultad.FACIL, 10, 1),
        Desafio("d2", "No uses redes sociales por 2 horas", "Dos horas sin redes.", 120, Dificultad.NORMAL, 50, 2),
        Desafio("d3", "No uses redes sociales por 8 horas", "Ocho horas sin redes.", 480, Dificultad.DIFICIL, 200, 3),
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
        compose.onNodeWithText("No uses redes sociales por 30 minutos").assertIsDisplayed()
        compose.onNodeWithText("Media hora sin redes.").assertIsDisplayed()
        compose.onNodeWithText("Duración: 30 minutos").assertIsDisplayed()
        compose.onNodeWithText("10 puntos").assertIsDisplayed()

        compose.onNodeWithText("No uses redes sociales por 2 horas").assertIsDisplayed()
        compose.onNodeWithText("Duración: 2 horas").assertIsDisplayed()
        compose.onNodeWithText("50 puntos").assertIsDisplayed()
    }

    @Test
    fun existenLasTresDificultadesComoTexto() {
        mostrar(DesafiosUiState.Lista(desafios))

        compose.onNodeWithText("Fácil").assertIsDisplayed()
        compose.onNodeWithText("Normal").assertIsDisplayed()
        compose.onNodeWithText("Difícil").assertIsDisplayed()
    }

    @Test
    fun noExisteNingunBotonParaIniciarUnDesafio() {
        mostrar(DesafiosUiState.Lista(desafios))

        // FR-017: solo se consulta; ninguna tarjeta ni control de la lista es accionable.
        compose.onAllNodes(hasClickAction()).assertCountEquals(0)
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
