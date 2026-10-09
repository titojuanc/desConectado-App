package com.desconectado.app.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithText
import com.desconectado.app.ui.challenges.BarraDesafioActivo
import com.desconectado.app.ui.navigation.MainShell
import com.desconectado.app.domain.model.ActiveChallenge
import com.desconectado.app.domain.model.Conectividad
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.desconectado.app.domain.model.ChallengeResult
import com.desconectado.app.ui.challenges.DesafioActivoScreen
import com.desconectado.app.ui.challenges.DesafioActivoUiState
import com.desconectado.app.ui.theme.DesConectadoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class DesafioActivoScreenTest {
    @get:Rule val compose = createComposeRule()

    private val resultado = ChallengeResult(
        challengeRunId = "run-1",
        challengeId = "challenge-1",
        challengeTitle = "Salir a caminar",
        durationMinutes = 30,
        startedAt = Instant.parse("2026-10-07T10:00:00Z"),
        finishedAt = Instant.parse("2026-10-07T10:30:00Z"),
        status = ChallengeResult.Status.COMPLETED,
        measuredSocialSeconds = 0,
        offlineSeconds = 0,
        pointsAwarded = 10,
    )

    private fun mostrar(
        estado: DesafioActivoUiState,
        onSeleccionarCalificacion: (Int) -> Unit = {},
        onCalificar: () -> Unit = {},
        onVolverCatalogo: () -> Unit = {},
    ) = compose.setContent {
        DesConectadoTheme {
            DesafioActivoScreen(
                estado = estado,
                onAbrirAjustes = {},
                onReintentarPermiso = {},
                onActualizar = {},
                onFinalizar = {},
                onCancelar = {},
                onSeleccionarCalificacion = onSeleccionarCalificacion,
                onCalificar = onCalificar,
                onVolverCatalogo = onVolverCatalogo,
            )
        }
    }

    @Test
    fun barraTieneContadorCruzYSigueVisibleAlCambiarPestana() {
        var cancelaciones = 0
        val active = ActiveChallenge("test", "Desafio de prueba", 2, 1, Instant.now(), 0,
            ActiveChallenge.Status.ACTIVE, Instant.now(), durationSeconds = 120)
        compose.setContent {
            DesConectadoTheme {
                MainShell(
                    conectividad = Conectividad.CONECTADO,
                    barraDesafio = { BarraDesafioActivo(active, 65, { cancelaciones++ }) },
                )
            }
        }
        compose.onNodeWithText("01:05").assertIsDisplayed()
        compose.onAllNodesWithTag("boton_actualizar_desafio").assertCountEquals(0)
        compose.onAllNodesWithTag("boton_finalizar_desafio").assertCountEquals(0)
        compose.onNodeWithTag("tab_recompensas").performClick()
        compose.onNodeWithTag("barra_desafio_activo").assertIsDisplayed()
        compose.onNodeWithTag("tab_perfil").performClick()
        compose.onNodeWithTag("barra_desafio_activo").assertIsDisplayed()
        compose.onNodeWithTag("boton_cancelar_desafio").performClick()
        assertEquals(1, cancelaciones)
    }

    @Test
    fun desafioCompletadoExigeCalificacionAntesDeVolver() {
        mostrar(DesafioActivoUiState.Terminado(resultado))

        compose.onNodeWithTag("rating_estrellas").assertIsDisplayed()
        compose.onNodeWithTag("boton_confirmar_calificacion").assertIsNotEnabled()
        compose.onAllNodesWithTag("boton_volver_catalogo").assertCountEquals(0)
    }

    @Test
    fun permiteConfirmarLaPuntuacionSeleccionada() {
        var puntuacion = 0
        var envios = 0
        mostrar(
            DesafioActivoUiState.Terminado(resultado, ratingSeleccionado = 4),
            onSeleccionarCalificacion = { puntuacion = it },
            onCalificar = { envios++ },
        )

        compose.onNodeWithTag("boton_rating_3").performClick()
        assertEquals(3, puntuacion)
        compose.onNodeWithTag("boton_confirmar_calificacion").performClick()
        assertEquals(1, envios)
    }

    @Test
    fun resultadosNoCompletadosNoPidenCalificacion() {
        mostrar(DesafioActivoUiState.Terminado(resultado.copy(status = ChallengeResult.Status.CANCELLED)))

        compose.onAllNodesWithTag("rating_estrellas").assertCountEquals(0)
        compose.onNodeWithTag("boton_volver_catalogo").assertIsDisplayed()
    }

    @Test
    fun calificacionGuardadaPermiteVolver() {
        mostrar(DesafioActivoUiState.Terminado(resultado, ratingStars = 5))

        compose.onAllNodesWithTag("rating_estrellas").assertCountEquals(0)
        compose.onNodeWithTag("boton_volver_catalogo").assertIsDisplayed()
    }
}