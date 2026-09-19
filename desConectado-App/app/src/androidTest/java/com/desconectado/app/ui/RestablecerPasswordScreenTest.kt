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
import com.desconectado.app.domain.ErrorCampo
import com.desconectado.app.ui.auth.IngresoScreen
import com.desconectado.app.ui.auth.IngresoUiState
import com.desconectado.app.ui.auth.RestablecerPasswordScreen
import com.desconectado.app.ui.auth.RestablecerPasswordUiState
import com.desconectado.app.ui.theme.DesConectadoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RestablecerPasswordScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun texto(id: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id)

    private fun mostrar(estado: RestablecerPasswordUiState, onEnviar: () -> Unit = {}) = compose.setContent {
        DesConectadoTheme {
            RestablecerPasswordScreen(
                estado = estado,
                onEmailChange = {},
                onEnviar = onEnviar,
                onVolver = {},
            )
        }
    }

    @Test
    fun ingreso_muestraElEnlaceOlvideMiContrasenaYLoInvoca() {
        var toques = 0
        compose.setContent {
            DesConectadoTheme {
                IngresoScreen(
                    estado = IngresoUiState(),
                    onEmailChange = {},
                    onPasswordChange = {},
                    onIngresar = {},
                    onIrARegistro = {},
                    onOlvidePassword = { toques++ },
                    onGoogle = {},
                )
            }
        }

        compose.onNodeWithTag("enlace_olvide_password").assertIsDisplayed().performClick()
        compose.onNodeWithText("Olvidé mi contraseña").assertIsDisplayed()

        assertEquals(1, toques)
    }

    @Test
    fun laPantallaMuestraElCampoDeCorreoYElBotonDeEnvio() {
        mostrar(RestablecerPasswordUiState())

        compose.onNodeWithTag("campo_correo_restablecer").assertIsDisplayed()
        compose.onNodeWithTag("boton_enviar_restablecimiento").assertIsDisplayed()
        compose.onNodeWithTag("mensaje_restablecimiento_enviado").assertDoesNotExist()
    }

    @Test
    fun trasEnviar_apareceElMensajeDeConfirmacionEnVoseo() {
        mostrar(RestablecerPasswordUiState(email = "ana@mail.com", confirmado = true))

        compose.onNodeWithTag("mensaje_restablecimiento_enviado").assertIsDisplayed()
        compose.onNodeWithText(
            "Si el correo está registrado, te enviamos un enlace para elegir una contraseña nueva.",
        ).assertIsDisplayed()
    }

    @Test
    fun elErrorDeFormatoApareceJuntoAlCampo() {
        mostrar(RestablecerPasswordUiState(email = "sin-arroba", errorCorreo = ErrorCampo.FORMATO))

        compose.onNodeWithText(texto(R.string.error_correo_formato)).assertIsDisplayed()
    }

    @Test
    fun sinConexion_elBotonSeDeshabilitaYApareceElAviso() {
        mostrar(RestablecerPasswordUiState(sinConexion = true))

        compose.onNodeWithTag("aviso_sin_conexion").assertIsDisplayed()
        compose.onNodeWithTag("boton_enviar_restablecimiento").assertIsNotEnabled()
    }

    @Test
    fun conUnEnvioEnCurso_elBotonSeDeshabilita() {
        mostrar(RestablecerPasswordUiState(email = "ana@mail.com", enviando = true))

        compose.onNodeWithTag("boton_enviar_restablecimiento").assertIsNotEnabled()
    }
}
