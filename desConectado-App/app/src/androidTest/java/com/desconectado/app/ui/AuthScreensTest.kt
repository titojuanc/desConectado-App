package com.desconectado.app.ui

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.desconectado.app.R
import com.desconectado.app.domain.ErrorCampo
import com.desconectado.app.domain.ErroresRegistro
import com.desconectado.app.domain.model.Conectividad
import com.desconectado.app.domain.model.EstadoSesion
import com.desconectado.app.ui.auth.IngresoScreen
import com.desconectado.app.ui.auth.IngresoUiState
import com.desconectado.app.ui.auth.RegistroScreen
import com.desconectado.app.ui.auth.RegistroUiState
import com.desconectado.app.ui.navigation.RaizApp
import com.desconectado.app.ui.theme.DesConectadoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Pantallas de acceso con composables sin Firebase (estado y acciones por parámetro). */
@RunWith(AndroidJUnit4::class)
class AuthScreensTest {

    @get:Rule
    val compose = createComposeRule()

    private fun texto(id: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id)

    private fun mostrarIngreso(estado: IngresoUiState = IngresoUiState()) = compose.setContent {
        DesConectadoTheme {
            IngresoScreen(
                estado = estado,
                onEmailChange = {},
                onPasswordChange = {},
                onIngresar = {},
                onIrARegistro = {},
                onOlvidePassword = {},
                onGoogle = {},
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
    fun ingreso_muestraElNombreDeLaApp() {
        mostrarIngreso()

        compose.onNodeWithText("(des)Conectado").assertIsDisplayed()
        compose.onNodeWithText(texto(R.string.app_name)).assertIsDisplayed()
    }

    @Test
    fun registro_muestraElNombreDeLaApp() {
        mostrarRegistro()

        compose.onNodeWithText("(des)Conectado").assertIsDisplayed()
    }

    @Test
    fun elCampoDeContrasenaEstaOculto() {
        mostrarIngreso()
        compose.onNodeWithTag("campo_password")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
        compose.onNodeWithTag("campo_password").performTextInput("Secreto123")

        compose.onNodeWithText("Secreto123").assertDoesNotExist()
    }

    @Test
    fun registro_elCampoDeContrasenaTambienEstaOculto() {
        mostrarRegistro()

        compose.onNodeWithTag("campo_password")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
    }

    @Test
    fun unCampoInvalido_muestraSuMensajeJuntoAlCampo() {
        mostrarRegistro(
            RegistroUiState(
                errores = ErroresRegistro(
                    username = ErrorCampo.MUY_CORTO,
                    email = ErrorCampo.FORMATO,
                    password = ErrorCampo.MUY_CORTO,
                ),
            ),
        )

        compose.onNodeWithText(texto(R.string.error_username_corto)).assertIsDisplayed()
        compose.onNodeWithText(texto(R.string.error_correo_formato)).assertIsDisplayed()
        compose.onNodeWithText(texto(R.string.error_password_corto)).assertIsDisplayed()
    }

    @Test
    fun sinConexion_losBotonesSeDeshabilitanYApareceElAviso() {
        mostrarIngreso(IngresoUiState(sinConexion = true))
        compose.onNodeWithTag("aviso_sin_conexion").assertIsDisplayed()
        compose.onNodeWithTag("boton_ingresar").assertIsNotEnabled()
    }

    @Test
    fun registro_sinConexion_elBotonSeDeshabilitaYApareceElAviso() {
        mostrarRegistro(RegistroUiState(sinConexion = true))

        compose.onNodeWithTag("aviso_sin_conexion").assertIsDisplayed()
        compose.onNodeWithTag("boton_registrar").assertIsNotEnabled()
    }

    @Test
    fun mientrasSeEnvia_elBotonSeDeshabilita() {
        mostrarIngreso(IngresoUiState(enviando = true))

        compose.onNodeWithTag("boton_ingresar").assertIsNotEnabled()
    }

    @Test
    fun credencialesInvalidas_muestraUnUnicoMensajeGenerico() {
        mostrarIngreso(
            IngresoUiState(errorEnvio = com.desconectado.app.domain.model.ErrorApp.CredencialesInvalidas),
        )

        compose.onNodeWithText(texto(R.string.error_credenciales)).assertIsDisplayed()
    }

    // --- FR-012: qué es alcanzable con y sin sesión ---

    private fun mostrarRaiz(estado: EstadoSesion, conectividad: Conectividad = Conectividad.CONECTADO) =
        compose.setContent {
            DesConectadoTheme {
                RaizApp(
                    estado = estado,
                    conectividad = conectividad,
                    sinSesion = {
                        IngresoScreen(
                            estado = IngresoUiState(),
                            onEmailChange = {},
                            onPasswordChange = {},
                            onIngresar = {},
                            onIrARegistro = {},
                            onOlvidePassword = {},
                onGoogle = {},
                        )
                    },
                )
            }
        }

    @Test
    fun sinSesion_soloSeMuestraElAcceso() {
        mostrarRaiz(EstadoSesion.SinSesion)

        compose.onNodeWithTag("campo_correo").assertIsDisplayed()
        compose.onNodeWithTag("tab_desafios").assertDoesNotExist()
        compose.onNodeWithTag("tab_recompensas").assertDoesNotExist()
        compose.onNodeWithTag("tab_perfil").assertDoesNotExist()
    }

    @Test
    fun conSesion_noSeMuestranIngresoNiRegistro() {
        mostrarRaiz(EstadoSesion.ConSesion("uid-1"))

        compose.onNodeWithTag("tab_desafios").assertIsDisplayed()
        compose.onNodeWithTag("tab_recompensas").assertIsDisplayed()
        compose.onNodeWithTag("tab_perfil").assertIsDisplayed()
        compose.onNodeWithTag("campo_correo").assertDoesNotExist()
        compose.onNodeWithTag("boton_ingresar").assertDoesNotExist()
        compose.onNodeWithTag("enlace_registro").assertDoesNotExist()
        compose.onNodeWithTag("boton_registrar").assertDoesNotExist()
    }

    @Test
    fun conSesionYSinConexion_seEntraIgualConElAvisoVisible() {
        mostrarRaiz(EstadoSesion.ConSesion("uid-1"), Conectividad.SIN_CONEXION)

        compose.onNodeWithTag("tab_desafios").assertIsDisplayed()
        compose.onNodeWithTag("aviso_sin_conexion").assertIsDisplayed()
    }

    @Test
    fun mientrasSeDeterminaLaSesion_seMuestraLaEspera() {
        mostrarRaiz(EstadoSesion.Cargando)

        compose.onNodeWithText("(des)Conectado").assertIsDisplayed()
        compose.onNodeWithTag("campo_correo").assertDoesNotExist()
        compose.onNodeWithTag("tab_desafios").assertDoesNotExist()
    }
}
