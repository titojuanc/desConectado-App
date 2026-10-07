package com.desconectado.app.ui

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.desconectado.app.R
import com.desconectado.app.domain.model.Recompensa
import com.desconectado.app.domain.model.PendingRedemption
import com.desconectado.app.domain.model.TipoRecompensa
import com.desconectado.app.ui.rewards.RecompensasScreen
import com.desconectado.app.ui.rewards.RecompensasUiState
import com.desconectado.app.ui.theme.DesConectadoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecompensasScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val recompensas = listOf(
        Recompensa("r1", "Primer paso", "Insignia digital dentro de la app.", 50, TipoRecompensa.INSIGNIA, 1),
        Recompensa("r2", "Tema de color para la app", "Cambia los colores de la app.", 100, TipoRecompensa.TEMA, 2),
    )

    private fun texto(id: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id)

    private fun mostrar(estado: RecompensasUiState, pendiente: PendingRedemption? = null) = compose.setContent {
        DesConectadoTheme {
            RecompensasScreen(estado = estado, onReintentar = {}, onCanjear = {}, canjePendiente = pendiente)
        }
    }

    @Test
    fun cadaTarjeta_muestraNombreDescripcionYCostoEnPuntos() {
        mostrar(RecompensasUiState.Lista(recompensas))

        compose.onNodeWithTag("lista_recompensas").assertIsDisplayed()
        compose.onNodeWithText("Primer paso").assertIsDisplayed()
        compose.onNodeWithText("Insignia digital dentro de la app.").assertIsDisplayed()
        compose.onNodeWithText("Costo: 50 puntos").assertIsDisplayed()
        compose.onNodeWithText("Tema de color para la app").assertIsDisplayed()
        compose.onNodeWithText("Costo: 100 puntos").assertIsDisplayed()
    }

    @Test
    fun noExisteNingunaAccionDeCanje() {
        mostrar(RecompensasUiState.Lista(recompensas))

        // FR-020: solo se consulta; ninguna tarjeta ni control de la lista es accionable.
        compose.onAllNodes(hasClickAction()).assertCountEquals(0)
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
        compose.onNodeWithTag("boton_canjear_r1").assertIsNotEnabled()
    }
}
