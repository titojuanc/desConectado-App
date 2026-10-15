package com.desconectado.app.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onNodeWithText
import com.desconectado.app.ui.challenges.BarraDesafioActivo
import com.desconectado.app.ui.navigation.MainShell
import com.desconectado.app.domain.model.ActiveChallenge
import com.desconectado.app.domain.model.Conectividad
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.desconectado.app.domain.model.ChallengeResult
import com.desconectado.app.domain.model.AchievementCriterion
import com.desconectado.app.domain.model.AchievementDefinition
import com.desconectado.app.domain.model.AchievementProgress
import com.desconectado.app.ui.challenges.DesafioActivoScreen
import com.desconectado.app.ui.challenges.DesafioActivoUiState
import com.desconectado.app.ui.challenges.DialogoDesafioEnProceso
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
    fun completandoMuestraIndicadorSinPuntosNiCalificacionPrematuros() {
        mostrar(DesafioActivoUiState.Completando)

        compose.onNodeWithText("Completando el desafío...").assertIsDisplayed()
        compose.onNodeWithTag("indicador_cargando").assertIsDisplayed()
        compose.onAllNodesWithTag("rating_estrellas").assertCountEquals(0)
        compose.onAllNodesWithTag("boton_volver_catalogo").assertCountEquals(0)
    }

    @Test
    fun iniciandoMuestraFeedbackDeLaSolicitudDelCatalogo() {
        mostrar(DesafioActivoUiState.Iniciando)

        compose.onNodeWithText("Iniciando el desafío...").assertIsDisplayed()
        compose.onNodeWithTag("indicador_cargando").assertIsDisplayed()
    }

    @Test
    fun errorDeCierrePermiteReintentarSinMostrarExito() {
        var reintentos = 0
        compose.setContent {
            DesConectadoTheme {
                DesafioActivoScreen(
                    estado = DesafioActivoUiState.Error(com.desconectado.app.domain.model.ErrorApp.Desconocido, alFinalizar = true),
                    onAbrirAjustes = {}, onReintentarPermiso = {}, onActualizar = {},
                    onFinalizar = { reintentos++ }, onCancelar = {},
                    onSeleccionarCalificacion = {}, onCalificar = {}, onVolverCatalogo = {},
                )
            }
        }
        compose.onNodeWithTag("boton_reintentar_cierre").performClick()
        assertEquals(1, reintentos)
        compose.onNodeWithTag("rating_estrellas").assertDoesNotExist()
    }

    @Test
    fun procesamientoSeMuestraSobreElCatalogoYNoSeCierraConAtras() {
        compose.setContent {
            DesConectadoTheme {
                MainShell(conectividad = Conectividad.CONECTADO)
                DialogoDesafioEnProceso(completando = true)
            }
        }

        compose.onNodeWithTag("pantalla_desafio_en_proceso").assertIsDisplayed()
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        compose.waitForIdle()
        compose.onNodeWithText("Completando el desafío...").assertIsDisplayed()
        compose.onNodeWithTag("indicador_cargando").assertIsDisplayed()
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
        compose.onNodeWithTag("boton_volver_catalogo").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun falloPorUsoDeAppsMuestraMotivoYSugerenciaEnLugarDelMensajeDePuntos() {
        var vueltas = 0
        mostrar(
            DesafioActivoUiState.Terminado(resultado.copy(
                status = ChallengeResult.Status.FAILED, measuredSocialSeconds = 12, pointsAwarded = 0,
            )),
            onVolverCatalogo = { vueltas++ },
        )

        compose.onNodeWithTag("resultado_desafio_no_completado").assertIsDisplayed()
        compose.onNodeWithText("Esta vez no se pudo").assertIsDisplayed()
        compose.onNodeWithText("Salir a caminar").assertIsDisplayed()
        compose.onNodeWithText("Se detectaron 12 segundos de uso de aplicaciones no permitidas durante el desafío.")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Desafío no cumplido. No se acreditaron puntos.").assertDoesNotExist()
        compose.onNodeWithTag("rating_estrellas").assertDoesNotExist()
        compose.onNodeWithTag("boton_volver_catalogo").performScrollTo().performClick()
        assertEquals(1, vueltas)
    }

    @Test
    fun invalidadoExplicaElLimiteDeDesconexion() {
        mostrar(DesafioActivoUiState.Terminado(resultado.copy(
            status = ChallengeResult.Status.INVALIDATED, offlineSeconds = 301, pointsAwarded = 0,
        )))

        compose.onNodeWithText("No pudimos validar el desafío").assertIsDisplayed()
        compose.onNodeWithText("Tiempo sin conexión: 5 min 1 s. Se superó el límite de 5 minutos permitido para verificar el desafío.")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("boton_confirmar_calificacion").assertDoesNotExist()
    }

    @Test
    fun falloAnticipadoExplicaQueNoSeCompletoElTiempo() {
        mostrar(DesafioActivoUiState.Terminado(resultado.copy(
            status = ChallengeResult.Status.FAILED,
            finishedAt = resultado.startedAt.plusSeconds(60), pointsAwarded = 0,
        )))

        compose.onNodeWithText("El desafío terminó antes de que se completara el tiempo necesario.")
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun canceladoExplicaLaDecisionSinAtribuirUsoDeApps() {
        mostrar(DesafioActivoUiState.Terminado(resultado.copy(
            status = ChallengeResult.Status.CANCELLED, measuredSocialSeconds = 12, pointsAwarded = 0,
        )))

        compose.onNodeWithText("Desafío cancelado").assertIsDisplayed()
        compose.onNodeWithText("Elegiste terminar el desafío antes de completarlo.")
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun resultadoSinDatosSuficientesNoInventaUnaCausa() {
        mostrar(DesafioActivoUiState.Terminado(resultado.copy(
            status = ChallengeResult.Status.FAILED, pointsAwarded = 0,
        )))

        compose.onNodeWithText("No contamos con una comprobación válida de las condiciones del desafío.")
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun invalidadoPorAppNoSePresentaComoProblemaDeInternet() {
        mostrar(DesafioActivoUiState.Terminado(resultado.copy(
            status = ChallengeResult.Status.INVALIDATED, pointsAwarded = 0,
            invalidationReason = "social_app_used",
        )))

        compose.onNodeWithText("Se detectó el uso de una aplicación no permitida durante el desafío.")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Volvé a intentarlo dejando las redes, los juegos y las demás apps no permitidas para después.")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Revisá tu conexión antes de empezar de nuevo. Todavía podés encontrar un momento para desconectar.")
            .assertDoesNotExist()
    }

    @Test
    fun invalidadoPorPermisoExplicaComoRecuperarElAcceso() {
        mostrar(DesafioActivoUiState.Terminado(resultado.copy(
            status = ChallengeResult.Status.INVALIDATED, pointsAwarded = 0,
            invalidationReason = "usage_access_revoked",
        )))

        compose.onNodeWithText("El permiso de acceso al uso de aplicaciones dejó de estar disponible durante el desafío y no pudimos seguir verificándolo.")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Revisá el acceso al uso de (des)Conectado en Ajustes antes de iniciar otro desafío.")
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun invalidadoHistoricoSinMotivoNoSuponeQueFaltoInternet() {
        mostrar(DesafioActivoUiState.Terminado(resultado.copy(
            status = ChallengeResult.Status.INVALIDATED, pointsAwarded = 0,
        )))

        compose.onNodeWithText("No contamos con una comprobación válida de las condiciones del desafío.")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Revisá tu conexión antes de empezar de nuevo. Todavía podés encontrar un momento para desconectar.")
            .assertDoesNotExist()
    }

    @Test
    fun calificacionGuardadaPermiteVolver() {
        mostrar(DesafioActivoUiState.Terminado(resultado, ratingStars = 5))

        compose.onAllNodesWithTag("rating_estrellas").assertCountEquals(0)
        compose.onNodeWithTag("boton_volver_catalogo").assertIsDisplayed()
    }

    @Test
    fun logroNuevoMuestraNotificacionDentroDeLaApp() {
        val logro = AchievementProgress(
            AchievementDefinition(
                id = "first-step",
                name = "Primer paso",
                description = "Completa tu primer desafío.",
                criterion = AchievementCriterion.COMPLETED_CHALLENGES,
                threshold = 1,
                order = 1,
            ),
            progress = 1,
        )
        mostrar(DesafioActivoUiState.Terminado(resultado, logrosDesbloqueados = listOf(logro)))

        compose.onNodeWithText("¡Logro desbloqueado: Primer paso!").assertIsDisplayed()
    }
}