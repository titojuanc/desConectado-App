package com.desconectado.app.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
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