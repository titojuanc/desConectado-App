package com.desconectado.app.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.desconectado.app.R
import com.desconectado.app.ui.auth.AccionesVinculacion
import com.desconectado.app.ui.auth.IngresoScreen
import com.desconectado.app.ui.auth.IngresoUiState
import com.desconectado.app.ui.auth.RegistroScreen
import com.desconectado.app.ui.auth.RegistroUiState
import com.desconectado.app.ui.auth.VinculacionPendiente
import com.desconectado.app.ui.theme.DesConectadoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GoogleScreensTest {

    @get:Rule
    val compose = createComposeRule()

    private fun texto(id: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id)

    private fun mostrarIngreso(estado: IngresoUiState = IngresoUiState(), onGoogle: () -> Unit = {}) =
        compose.setContent {
            DesConectadoTheme {
                IngresoScreen(
                    estado = estado,
                    onEmailChange = {},
                    onPasswordChange = {},
                    onIngresar = {},
                    onIrARegistro = {},
                    onOlvidePassword = {},
                    onGoogle = onGoogle,
                    vinculacion = AccionesVinculacion(),
                )
            }
        }

    private fun mostrarRegistro(estado: RegistroUiState = RegistroUiState()) = compose.setContent {
        DesConectadoTheme {
            RegistroScreen(
                estado = estado,
                onUsernameChange = {},
                onEmailChange = {},
                onPasswordChange = {},
                onRegistrar = {},
                onIrAIngreso = {},
                onGoogle = {},
            )
        }
    }

    @Test
    fun ingreso_muestraElBotonDeGoogleYLoInvoca() {
        var toques = 0
        mostrarIngreso(onGoogle = { toques++ })

        compose.onNodeWithTag("boton_google").assertIsDisplayed().performClick()
        compose.onNodeWithText("Continuar con Google").assertIsDisplayed()

        assertEquals(1, toques)
    }

    @Test
    fun registro_muestraElBotonDeGoogle() {
        mostrarRegistro()

        compose.onNodeWithTag("boton_google").assertIsDisplayed()
        compose.onNodeWithText("Continuar con Google").assertIsDisplayed()
    }

    @Test
    fun ingreso_sinConexion_elBotonDeGoogleSeDeshabilita() {
        mostrarIngreso(IngresoUiState(sinConexion = true))

        compose.onNodeWithTag("boton_google").assertIsNotEnabled()
    }

    @Test
    fun registro_sinConexion_elBotonDeGoogleSeDeshabilita() {
        mostrarRegistro(RegistroUiState(sinConexion = true))

        compose.onNodeWithTag("boton_google").assertIsNotEnabled()
    }

    @Test
    fun elDialogoDeVinculacion_muestraElMensajeYPideLaContrasena() {
        mostrarIngreso(IngresoUiState(vinculacion = VinculacionPendiente(idToken = "t", email = "ana@mail.com")))

        compose.onNodeWithTag("dialogo_vinculacion").assertIsDisplayed()
        compose.onNodeWithText(texto(R.string.vinculacion_mensaje)).assertIsDisplayed()
        compose.onNodeWithText("Ya tenés una cuenta con este correo", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("campo_password_vincular").assertIsDisplayed()
        compose.onNodeWithTag("boton_vincular").assertIsDisplayed()
    }

    @Test
    fun elDialogoDeVinculacion_tambienApareceEnRegistro() {
        mostrarRegistro(RegistroUiState(vinculacion = VinculacionPendiente(idToken = "t")))

        compose.onNodeWithTag("dialogo_vinculacion").assertIsDisplayed()
        compose.onNodeWithTag("campo_password_vincular").assertIsDisplayed()
    }

    @Test
    fun sinVinculacionPendiente_noHayDialogo() {
        mostrarIngreso()

        compose.onNodeWithTag("dialogo_vinculacion").assertDoesNotExist()
    }
}
