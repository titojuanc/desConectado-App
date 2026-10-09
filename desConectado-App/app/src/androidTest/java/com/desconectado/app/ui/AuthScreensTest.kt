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
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertTrue
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
import org.junit.Assert.assertEquals
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

    private fun mostrarRegistro(
        estado: RegistroUiState = RegistroUiState(),
        onMetaSemanalChange: (Int?) -> Unit = {},
    ) = compose.setContent {
        DesConectadoTheme {
            RegistroScreen(
                estado = estado,
                onUsernameChange = {},
                onEmailChange = {},
                onPasswordChange = {},
                onRegistrar = {},
                onIrAIngreso = {},
                onGoogle = {},
                onMetaSemanalChange = onMetaSemanalChange,
            )
        }
    }

    @Test
    fun permiteMostrarYOcultarLaContrasenaSinModificarla() {
        mostrarIngreso(IngresoUiState(password = "ClaveDePrueba123"))
        compose.onNodeWithTag("campo_password").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
        val hidden = compose.onNodeWithTag("campo_password").fetchSemanticsNode().config[SemanticsProperties.EditableText].text
        compose.onNodeWithTag("visibilidad_campo_password").performClick()
        assertEquals("ClaveDePrueba123", compose.onNodeWithTag("campo_password").fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        compose.onNodeWithTag("visibilidad_campo_password").performClick()
        compose.onNodeWithTag("campo_password").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
        assertEquals(hidden, compose.onNodeWithTag("campo_password").fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
    }

    @Test
    fun capturaAccesoYRegistroConLogoYTeclado() {
        val registro = mutableStateOf(false)
        val email = mutableStateOf("")
        compose.setContent {
            DesConectadoTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    if (registro.value) {
                        RegistroScreen(RegistroUiState(weeklyGoalMinutes = 120), onUsernameChange = {}, onEmailChange = {}, onPasswordChange = {},
                            onRegistrar = {}, onIrAIngreso = { registro.value = false }, onGoogle = {})
                    } else {
                        IngresoScreen(IngresoUiState(email = email.value, weeklyGoalMinutes = 120), onEmailChange = { email.value = it },
                            onPasswordChange = {}, onIngresar = {}, onIrARegistro = { registro.value = true }, onOlvidePassword = {}, onGoogle = {})
                    }
                }
            }
        }
        compose.onNodeWithTag("logo_acceso").assertIsDisplayed()
        capturaAcceso("ingreso")
        compose.onNodeWithTag("campo_correo").performClick().performTextInput("qa@example.invalid")
        compose.onNodeWithTag("boton_ingresar").performScrollTo().assertIsDisplayed()
        capturaAcceso("teclado")
        val hide = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("input keyevent 4")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(hide).use { it.readBytes() }
        compose.onNodeWithTag("enlace_registro").performScrollTo().performClick()
        compose.onNodeWithTag("campo_username").assertIsDisplayed()
        compose.onNodeWithTag("logo_acceso").assertIsDisplayed()
        capturaAcceso("registro")
        compose.onNodeWithTag("boton_registrar").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("enlace_ingreso").performScrollTo().assertIsDisplayed()
    }

    private fun capturaAcceso(name: String) {
        compose.waitForIdle()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val destination = File(ApplicationProvider.getApplicationContext<Context>().getExternalFilesDir(null), "acceso-$name.png")
        destination.outputStream().use { assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)) }
        val copy = automation.executeShellCommand("cp ${destination.absolutePath} /data/local/tmp/acceso-$name.png")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(copy).use { it.readBytes() }
        bitmap.recycle()
    }

    @Test
    fun iconoAdaptableUsaElNuevoSimboloSinElPuntoAnterior() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val icon = requireNotNull(context.getDrawable(R.mipmap.ic_launcher))
        val bitmap = android.graphics.Bitmap.createBitmap(192, 192, android.graphics.Bitmap.Config.ARGB_8888)
        icon.setBounds(0, 0, 192, 192)
        icon.draw(android.graphics.Canvas(bitmap))
        val center = bitmap.getPixel(96, 96)
        assertTrue(android.graphics.Color.green(center) < 100)
        var leftGreen = 0
        var rightGreen = 0
        for (row in 0 until bitmap.height) {
            for (column in 0 until bitmap.width) {
                val pixel = bitmap.getPixel(column, row)
                if (android.graphics.Color.green(pixel) > 120 && android.graphics.Color.green(pixel) > android.graphics.Color.red(pixel) + 25) {
                    if (column < 96) leftGreen++ else rightGreen++
                }
            }
        }
        assertTrue("Left parenthesis missing", leftGreen > 200)
        assertTrue("Right parenthesis missing", rightGreen > 200)
        val file = File(context.getExternalFilesDir(null), "acceso-icono.png")
        file.outputStream().use { assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)) }
        val copy = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("cp ${file.absolutePath} /data/local/tmp/acceso-icono.png")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(copy).use { it.readBytes() }
        bitmap.recycle()
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
    fun registro_exigeElegirUnaMetaYOfreceOpcionesEnIntervalosDeTreinta() {
        var metaElegida: Int? = null
        mostrarRegistro(onMetaSemanalChange = { metaElegida = it })

        compose.onNodeWithText("Elegí una meta semanal").assertIsDisplayed()
        compose.onNodeWithText("Elegir meta semanal").assertIsDisplayed()
        compose.onNodeWithTag("boton_registrar").assertIsNotEnabled()
        compose.onNodeWithTag("selector_meta_semanal").performClick()
        compose.onNodeWithText("180 min/semana").performClick()

        assertEquals(180, metaElegida)
    }

    @Test
    fun GoogleDesdeIngreso_exigeMetaParaPosibleCuentaNueva() {
        mostrarIngreso()

        compose.onNodeWithText("Meta semanal para una cuenta nueva de Google").assertIsDisplayed()
        compose.onNodeWithTag("selector_meta_semanal").assertIsDisplayed()
        compose.onNodeWithTag("boton_google").assertIsNotEnabled()
        compose.onNodeWithTag("boton_ingresar").assertIsDisplayed()
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
