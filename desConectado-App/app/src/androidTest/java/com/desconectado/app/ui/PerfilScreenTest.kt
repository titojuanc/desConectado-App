package com.desconectado.app.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.desconectado.app.domain.model.Perfil
import com.desconectado.app.ui.profile.PerfilScreen
import com.desconectado.app.ui.profile.PerfilUiState
import com.desconectado.app.ui.theme.DesConectadoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PerfilScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val perfil = Perfil(username = "Ana Prueba", email = "ana@mail.com")

    private fun mostrar(
        estado: PerfilUiState = PerfilUiState.Datos(perfil),
        onCerrarSesion: () -> Unit = {},
    ) = compose.setContent {
        DesConectadoTheme {
            PerfilScreen(estado = estado, onReintentar = {}, onCerrarSesion = onCerrarSesion)
        }
    }

    @Test
    fun muestraElNombreDeUsuarioYElCorreoRecibidos() {
        mostrar()

        compose.onNodeWithTag("texto_username").assertIsDisplayed().assertTextEquals("Ana Prueba")
        compose.onNodeWithTag("texto_email").assertIsDisplayed().assertTextEquals("ana@mail.com")
    }

    @Test
    fun existeElBotonParaCerrarSesionYLoInvoca() {
        var cierres = 0
        mostrar(onCerrarSesion = { cierres++ })

        compose.onNodeWithTag("boton_cerrar_sesion").assertIsDisplayed().performClick()

        assertEquals(1, cierres)
    }

    @Test
    fun noApareceNingunCampoNiTextoDeContrasena() {
        mostrar()

        compose.onNodeWithText("contraseña", substring = true, ignoreCase = true).assertDoesNotExist()
        compose.onNodeWithText("password", substring = true, ignoreCase = true).assertDoesNotExist()
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password)).assertCountEquals(0)
    }

    @Test
    fun cerrarSesionEstaDisponibleTambienSiElPerfilNoCargo() {
        mostrar(estado = PerfilUiState.Error)

        compose.onNodeWithTag("boton_cerrar_sesion").assertIsDisplayed()
        compose.onNodeWithTag("boton_reintentar").assertIsDisplayed()
    }
}
