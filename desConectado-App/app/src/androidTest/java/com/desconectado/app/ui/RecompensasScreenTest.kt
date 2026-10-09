package com.desconectado.app.ui

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.assertTextEquals
import com.desconectado.app.ui.points.SaldoUiState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.test.platform.app.InstrumentationRegistry
import com.desconectado.app.ui.navigation.MainShell
import com.desconectado.app.domain.model.Conectividad
import com.desconectado.app.domain.model.CategoriaDesafio
import com.desconectado.app.ui.rewards.CanjeFeedback
import kotlinx.coroutines.flow.MutableSharedFlow
import java.io.File
import org.junit.Assert.assertTrue
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.desconectado.app.R
import com.desconectado.app.domain.model.Recompensa
import com.desconectado.app.domain.model.CosmeticOwnership
import com.desconectado.app.domain.model.CosmeticPreferences
import com.desconectado.app.domain.model.PendingRedemption
import com.desconectado.app.domain.model.TipoRecompensa
import com.desconectado.app.ui.rewards.RecompensasScreen
import com.desconectado.app.ui.rewards.RecompensasUiState
import com.desconectado.app.ui.theme.DesConectadoTheme
import com.desconectado.app.ui.profile.ProgresoUiState
import com.desconectado.app.domain.ProgressMetrics
import com.desconectado.app.domain.model.AchievementCriterion
import com.desconectado.app.domain.model.AchievementDefinition
import com.desconectado.app.domain.model.AchievementProgress
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecompensasScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val recompensas = listOf(
        Recompensa("r1", "Cupón digital", "Cupón digital dentro de la app.", 50, TipoRecompensa.CUPON, 1),
        Recompensa("r2", "Tema de color para la app", "Cambia los colores de la app.", 100, TipoRecompensa.TEMA, 2),
    )

    private fun texto(id: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id)

    private fun mostrar(
        estado: RecompensasUiState,
        pendiente: PendingRedemption? = null,
        propiedad: List<CosmeticOwnership> = emptyList(),
        preferencias: CosmeticPreferences = CosmeticPreferences(),
        onAplicar: ((String) -> Unit)? = null,
        onQuitar: ((TipoRecompensa) -> Unit)? = null,
        saldo: SaldoUiState = SaldoUiState.Disponible(150),
        onCanjear: ((Recompensa) -> Unit)? = {},
    ) = compose.setContent {
        DesConectadoTheme {
            RecompensasScreen(
                estado = estado,
                onReintentar = {},
                onCanjear = onCanjear,
                saldo = saldo,
                canjePendiente = pendiente,
                propiedad = propiedad,
                preferencias = preferencias,
                onAplicarCosmetico = onAplicar,
                onQuitarCosmetico = onQuitar,
            )
        }
    }

    @Test
    fun separaCanjeablesDeLogrosSinBotonesDeCompraEnLogros() {
        val achievement = AchievementProgress(
            AchievementDefinition("quick", "Medalla inmediata", "Completa un desafio", AchievementCriterion.COMPLETED_CHALLENGES, 1, 1),
            1,
        )
        compose.setContent {
            DesConectadoTheme {
                RecompensasScreen(
                    estado = RecompensasUiState.Lista(recompensas),
                    onReintentar = {},
                    onCanjear = {},
                    saldo = SaldoUiState.Disponible(150),
                    logros = ProgresoUiState.Datos(ProgressMetrics(1, 10, 1, 10, null, emptyMap()), listOf(achievement)),
                )
            }
        }
        compose.onNodeWithTag("seccion_logros").performClick()
        compose.onNodeWithText("Medalla inmediata").assertIsDisplayed()
        compose.onNodeWithText("Desbloqueado").assertIsDisplayed()
        compose.onNodeWithText("1 de 1 desbloqueados").assertIsDisplayed()
        compose.onNodeWithTag("boton_canjear_r1").assertDoesNotExist()
        compose.onNodeWithTag("seccion_canjeables").performClick()
        compose.onNodeWithTag("lista_recompensas").performScrollToNode(hasTestTag("boton_canjear_r1"))
        compose.onNodeWithTag("boton_canjear_r1").assertIsDisplayed()
    }

    @Test
    fun cadaTarjeta_muestraNombreDescripcionYCostoEnPuntos() {
        mostrar(RecompensasUiState.Lista(recompensas))

        compose.onNodeWithTag("lista_recompensas").assertIsDisplayed()
        compose.onNodeWithText("Tema de color para la app").assertIsDisplayed()
        compose.onNodeWithTag("costo_recompensa_r2").assertTextEquals("100")
        compose.onNodeWithTag("imagen_recompensa_r2").assertIsDisplayed()
        compose.onNodeWithTag("lista_recompensas").performScrollToNode(hasTestTag("recompensa_r1"))
        compose.onNodeWithText("Cupón digital").assertIsDisplayed()
        compose.onNodeWithText("Cupón digital dentro de la app.").assertIsDisplayed()
        compose.onNodeWithTag("costo_recompensa_r1").assertTextEquals("50")
    }

    @Test
    fun sinCallbackNoOfreceCanjesPeroConservaLasSecciones() {
        mostrar(RecompensasUiState.Lista(recompensas), onCanjear = null)
        compose.onNodeWithTag("boton_canjear_r2").assertDoesNotExist()
        compose.onNodeWithTag("seccion_logros").assertIsDisplayed()
    }

    @Test
    fun aclaraQueLasRecompensasSonDigitalesYSoloDeLaApp() {
        mostrar(RecompensasUiState.Lista(recompensas))

        // FR-019: ningún texto sugiere beneficios fuera de la app.
        compose.onNodeWithText(texto(R.string.recompensas_aclaracion)).assertIsDisplayed()
        compose.onNodeWithText("comercio", substring = true, ignoreCase = true).assertDoesNotExist()
        compose.onNodeWithText("descuento real", substring = true, ignoreCase = true).assertDoesNotExist()
    }

    @Test
    fun elEstadoDeError_muestraReintentar() {
        mostrar(RecompensasUiState.Error)

        compose.onNodeWithTag("boton_reintentar").assertIsDisplayed()
    }

    @Test
    fun elEstadoSinConexion_explicaQueSeRequiereConexion() {
        mostrar(RecompensasUiState.SinConexion)

        compose.onNodeWithText(texto(R.string.estado_sin_conexion)).assertIsDisplayed()
        compose.onNodeWithTag("boton_reintentar").assertIsDisplayed()
    }

    @Test
    fun muestraElCanjePendienteYDeshabilitaNuevosCanjes() {
        val pendiente = PendingRedemption(
            redemptionId = "r-pending",
            rewardId = "r1",
            name = "Primer paso",
            costPoints = 50,
            code = null,
            pointsDebited = 20,
            lotDebits = mapOf("lot-1" to 20),
            createdAt = java.time.Instant.now(),
        )
        mostrar(RecompensasUiState.Lista(recompensas), pendiente)

        compose.onNodeWithTag("canje_pendiente").assertIsDisplayed()
        compose.onNodeWithText("20 de 50 puntos descontados; se va a reanudar cuando vuelvas a la tienda.").assertIsDisplayed()
        compose.onNodeWithTag("lista_recompensas").performScrollToNode(hasTestTag("boton_canjear_r1"))
        compose.onNodeWithTag("boton_canjear_r1").assertIsNotEnabled()
    }

    @Test
    fun muestraInventarioYPermiteAplicarOCambiarUnCosmeticoPoseido() {
        val bosque = CosmeticOwnership(
            rewardId = "theme-bosque",
            name = "Tema Bosque",
            kind = TipoRecompensa.TEMA,
            config = mapOf("palette" to "forest"),
            acquiredAt = java.time.Instant.parse("2026-10-08T10:00:00Z"),
            redemptionId = "purchase-1",
        )
        var aplicados = 0
        var retirados = 0
        val seleccion = mutableStateOf(CosmeticPreferences())
        compose.setContent {
            DesConectadoTheme {
                RecompensasScreen(RecompensasUiState.Lista(recompensas), onReintentar = {},
                    propiedad = listOf(bosque), preferencias = seleccion.value,
                    onAplicarCosmetico = { id -> if (id == bosque.rewardId) aplicados++; seleccion.value = CosmeticPreferences(mapOf(TipoRecompensa.TEMA to id)) },
                    onQuitarCosmetico = { if (it == TipoRecompensa.TEMA) retirados++ })
            }
        }
        compose.onNodeWithTag("lista_recompensas").performScrollToNode(hasTestTag("cosmetico_theme-bosque"))
        compose.onNodeWithTag("cosmetico_theme-bosque").assertIsDisplayed()
        compose.onNodeWithTag("boton_aplicar_theme-bosque").performClick()
        compose.onNodeWithTag("boton_canjear_theme-bosque").assertDoesNotExist()
        assertEquals(1, aplicados)

        compose.onNodeWithTag("boton_quitar_theme-bosque").performClick()
        assertEquals(1, retirados)
    }

    @Test
    fun capturaCanjeablesYLogrosConEstadosRealesDeUI() {
        val rewards = listOf(
            Recompensa("theme-bosque", "Tema Bosque", "Un estilo natural y tranquilo.", 100, TipoRecompensa.TEMA, 1, config = mapOf("palette" to "forest")),
            Recompensa("theme-atardecer", "Tema Atardecer", "Colores cálidos para desconectarte.", 150, TipoRecompensa.TEMA, 2, config = mapOf("palette" to "sunset")),
            Recompensa("theme-oceano", "Tema Océano", "Tonos azules para una experiencia más calma.", 150, TipoRecompensa.TEMA, 3, config = mapOf("palette" to "ocean")),
            Recompensa("theme-noche", "Tema Noche", "Modo oscuro con detalles estrellados.", 200, TipoRecompensa.TEMA, 4),
            Recompensa("background-montanas", "Montañas", "Un fondo para concentrarte.", 60, TipoRecompensa.FONDO_ENFOQUE, 5),
        )
        val medals = listOf(
            AchievementProgress(AchievementDefinition("primer-paso", "Primer paso", "Completaste tu primer desafío.", AchievementCriterion.COMPLETED_CHALLENGES, 1, 1), 1),
            AchievementProgress(AchievementDefinition("en-marcha", "En marcha", "Completa cinco desafíos.", AchievementCriterion.COMPLETED_CHALLENGES, 5, 2), 3),
            AchievementProgress(AchievementDefinition("foco-total", "Foco total", "Cinco desafíos de Enfocarme.", AchievementCriterion.CATEGORY_COMPLETIONS, 5, 3, CategoriaDesafio.ENFOCARME), 0),
        )
        val owned = CosmeticOwnership("theme-bosque", "Tema Bosque", TipoRecompensa.TEMA, mapOf("palette" to "forest"), java.time.Instant.EPOCH, "qa")
        compose.setContent {
            DesConectadoTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MainShell(conectividad = Conectividad.CONECTADO, saldo = SaldoUiState.Disponible(140), recompensas = {
                        RecompensasScreen(RecompensasUiState.Lista(rewards), onReintentar = {}, onCanjear = {}, saldo = SaldoUiState.Disponible(140),
                            propiedad = listOf(owned), preferencias = CosmeticPreferences(mapOf(TipoRecompensa.TEMA to "theme-bosque")),
                            onAplicarCosmetico = {}, onQuitarCosmetico = {},
                            logros = ProgresoUiState.Datos(ProgressMetrics(3, 1800, 1, 1800, null, emptyMap()), medals))
                    })
                }
            }
        }
        compose.onNodeWithTag("tab_recompensas").performClick()
        compose.onNodeWithTag("imagen_recompensa_theme-bosque").assertIsDisplayed()
        capturar("canjeables")
        compose.onNodeWithTag("seccion_logros").performClick()
        compose.onNodeWithText("1 de 3 desbloqueados").assertIsDisplayed()
        capturar("logros")
    }

    private fun capturar(name: String) {
        compose.waitForIdle()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val screenshot = requireNotNull(automation.takeScreenshot())
        val file = File(ApplicationProvider.getApplicationContext<Context>().getExternalFilesDir(null), "recompensas-$name.png")
        file.outputStream().use { assertTrue(screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)) }
        val copy = automation.executeShellCommand("cp ${file.absolutePath} /data/local/tmp/recompensas-$name.png")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(copy).use { it.readBytes() }
        screenshot.recycle()
    }

    @Test
    fun avisoDeCanjeExitosoActualizaSaldoYConservaFeedback() {
        val events = MutableSharedFlow<CanjeFeedback>(extraBufferCapacity = 1)
        var updates = 0
        compose.setContent {
            DesConectadoTheme {
                RecompensasScreen(RecompensasUiState.Lista(recompensas), onReintentar = {}, feedbackEvents = events,
                    onCanjeExitoso = { updates++ })
            }
        }
        compose.waitForIdle()
        compose.runOnIdle { events.tryEmit(CanjeFeedback.Exitoso) }
        compose.waitUntil { updates == 1 }
        compose.onNodeWithText(texto(R.string.canje_exitoso)).assertIsDisplayed()
    }

    @Test
    fun saldoInsuficienteODesconocidoNoPermiteCanjear() {
        mostrar(RecompensasUiState.Lista(recompensas), saldo = SaldoUiState.Disponible(40))
        compose.onNodeWithTag("saldo_insuficiente_r2").assertTextEquals("Te faltan 60 puntos")
        compose.onNodeWithTag("boton_canjear_r2").assertIsNotEnabled()
    }

    @Test
    fun saldoDesconocidoNoSePresentaComoCero() {
        mostrar(RecompensasUiState.Lista(recompensas), saldo = SaldoUiState.NoDisponible)
        compose.onNodeWithTag("boton_canjear_r2").assertIsNotEnabled()
        compose.onNodeWithTag("saldo_insuficiente_r2").assertDoesNotExist()
    }

    @Test
    fun canjearEnviaLaRecompensaSeleccionada() {
        var enviada: Recompensa? = null
        mostrar(RecompensasUiState.Lista(recompensas), onCanjear = { enviada = it })
        compose.onNodeWithTag("boton_canjear_r2").performClick()
        assertEquals(recompensas[1], enviada)
    }

    @Test
    fun logrosPendientesMuestranProgresoSinCanje() {
        val logro = AchievementProgress(AchievementDefinition("parcial", "En marcha", "Completa cinco desafíos.", AchievementCriterion.COMPLETED_CHALLENGES, 5, 1), 3)
        compose.setContent {
            DesConectadoTheme {
                RecompensasScreen(RecompensasUiState.Lista(recompensas), onReintentar = {},
                    logros = ProgresoUiState.Datos(ProgressMetrics(3, 1800, 1, 1800, null, emptyMap()), listOf(logro)))
            }
        }
        compose.onNodeWithTag("seccion_logros").performClick()
        compose.onNodeWithText("0 de 1 desbloqueados").assertIsDisplayed()
        compose.onNodeWithTag("lista_logros").performScrollToNode(hasTestTag("progreso_logro_parcial"))
        compose.onNodeWithText("3 de 5").assertIsDisplayed()
        compose.onNodeWithTag("boton_canjear_r2").assertDoesNotExist()
    }
}
