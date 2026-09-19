package com.desconectado.app.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.desconectado.app.BuildConfig
import com.desconectado.app.DesConectadoApp
import com.desconectado.app.MainActivity
import com.desconectado.app.util.EmuladorFirebase
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Flujo completo por la interfaz contra los emuladores: registro, perfil y cierre de sesión.
 * Requiere `-PuseEmulator=true` y los emuladores de Auth y Firestore activos.
 */
@RunWith(AndroidJUnit4::class)
class PerfilFlowEmulatorTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private var escenario: ActivityScenario<MainActivity>? = null

    @Before
    fun preparar() {
        assumeTrue("Requiere -PuseEmulator=true", BuildConfig.USE_FIREBASE_EMULATOR)
        val app = ApplicationProvider.getApplicationContext<Context>() as DesConectadoApp
        app.container.auth.signOut()
        EmuladorFirebase.limpiarAuth()
        escenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun cerrar() {
        escenario?.close()
    }

    private fun ComposeTestRule.esperarPorTag(etiqueta: String) {
        waitUntil(timeoutMillis = 20_000) {
            onAllNodesWithTag(etiqueta).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun registrarYVerElPerfilConLosDatosExactosSinContrasena() {
        compose.esperarPorTag("enlace_registro")
        compose.onNodeWithTag("enlace_registro").performClick()

        compose.esperarPorTag("campo_username")
        compose.onNodeWithTag("campo_username").performTextInput("Ana Prueba")
        compose.onNodeWithTag("campo_correo").performTextInput("ana@mail.com")
        compose.onNodeWithTag("campo_password").performTextInput("Secreto123")
        compose.onNodeWithTag("boton_registrar").performClick()

        compose.esperarPorTag("tab_perfil")
        compose.onNodeWithTag("tab_perfil").performClick()

        compose.esperarPorTag("texto_username")
        compose.onNodeWithTag("texto_username").assertTextEquals("Ana Prueba")
        compose.onNodeWithTag("texto_email").assertTextEquals("ana@mail.com")
        compose.onNodeWithText("Secreto123").assertDoesNotExist()

        compose.onNodeWithTag("boton_cerrar_sesion").assertIsDisplayed().performClick()

        compose.esperarPorTag("campo_correo")
        compose.onNodeWithTag("boton_ingresar").assertIsDisplayed()
    }
}
