package com.desconectado.app.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.desconectado.app.domain.model.DesafioHecho
import com.desconectado.app.domain.model.Perfil
import com.desconectado.app.ui.profile.DesafiosHechosUiState
import com.desconectado.app.ui.profile.PerfilScreen
import com.desconectado.app.ui.profile.PerfilUiState
import com.desconectado.app.ui.theme.DesConectadoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class PerfilScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val perfil = Perfil(username = "Ana Prueba", email = "ana@mail.com")

    private val desafiosHechos = listOf(
        DesafioHecho("Salir a trotar", 50, Instant.parse("2026-09-22T15:00:00Z")),
        DesafioHecho("Salir a caminar", 10, Instant.parse("2026-09-20T15:00:00Z")),
    )

    private fun mostrar(
        estado: PerfilUiState = PerfilUiState.Datos(perfil),
        onCerrarSesion: () -> Unit = {},
        desafios: DesafiosHechosUiState = DesafiosHechosUiState.Lista(emptyList()),
        onReintentarPuntos: () -> Unit = {},
    ) = compose.setContent {
        DesConectadoTheme {
            PerfilScreen(
                estado = estado,
                onReintentar = {},
                onCerrarSesion = onCerrarSesion,
                desafiosHechos = desafios,
                onReintentarPuntos = onReintentarPuntos,
            )
        }
    }

    private fun arriba(texto: String): Float =
        compose.onNodeWithText(texto, substring = true).fetchSemanticsNode().boundsInRoot.top

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

    // --- Puntos y últimos desafíos hechos (FR-027 a FR-033) ---

    @Test
    fun muestraElSaldoDePuntosConSeparadorDeMiles() {
        mostrar(estado = PerfilUiState.Datos(perfil.copy(puntos = 1250)))

        compose.onNodeWithTag("puntos_perfil").assertIsDisplayed().assertTextEquals("1.250")
    }

    @Test
    fun unaCuentaNuevaMuestraSaldoCeroYUnTextoExplicativoEnLugarDeUnEspacioVacio() {
        mostrar(estado = PerfilUiState.Datos(perfil), desafios = DesafiosHechosUiState.Lista(emptyList()))

        compose.onNodeWithTag("puntos_perfil").assertTextEquals("0")
        compose.onNodeWithTag("texto_sin_desafios_hechos").assertIsDisplayed()
        compose.onNodeWithTag("lista_desafios_hechos").assertDoesNotExist()
    }

    @Test
    fun muestraLosDesafiosHechosConTituloPuntosYFechaEnElOrdenRecibido() {
        mostrar(desafios = DesafiosHechosUiState.Lista(desafiosHechos))

        compose.onNodeWithTag("lista_desafios_hechos").assertIsDisplayed()
        compose.onNodeWithText("Salir a trotar").assertIsDisplayed()
        compose.onNodeWithText("+50 puntos", substring = true).assertIsDisplayed()
        compose.onNodeWithText("22/09/2026", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Salir a caminar").assertIsDisplayed()
        // Del más reciente al más antiguo: el primero queda arriba.
        assertTrue(arriba("Salir a trotar") < arriba("Salir a caminar"))
    }

    @Test
    fun unFalloDePuntos_muestraReintentarSinOcultarNombreNiCorreo() {
        var reintentos = 0
        mostrar(desafios = DesafiosHechosUiState.Error, onReintentarPuntos = { reintentos++ })

        compose.onNodeWithTag("texto_username").assertIsDisplayed()
        compose.onNodeWithTag("texto_email").assertIsDisplayed()
        compose.onNodeWithTag("boton_reintentar_puntos").assertIsDisplayed().performClick()
        assertEquals(1, reintentos)
    }

    @Test
    fun sinConexionEnPuntos_avisaQueSeRequiereConexionYNoMuestraUnaListaVacia() {
        mostrar(desafios = DesafiosHechosUiState.SinConexion)

        compose.onNodeWithTag("boton_reintentar_puntos").assertIsDisplayed()
        compose.onNodeWithTag("texto_sin_desafios_hechos").assertDoesNotExist()
        compose.onNodeWithTag("lista_desafios_hechos").assertDoesNotExist()
    }

    @Test
    fun noExisteNingunaAccionParaGanarGastarOModificarPuntos() {
        mostrar(
            estado = PerfilUiState.Datos(perfil.copy(puntos = 60)),
            desafios = DesafiosHechosUiState.Lista(desafiosHechos),
        )

        // FR-032: lo único accionable del perfil es cerrar sesión.
        compose.onAllNodes(hasClickAction()).assertCountEquals(1)
        compose.onNodeWithTag("boton_cerrar_sesion").assertIsDisplayed()
    }
}
