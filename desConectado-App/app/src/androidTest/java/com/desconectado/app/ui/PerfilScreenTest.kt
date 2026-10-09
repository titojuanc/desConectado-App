package com.desconectado.app.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.desconectado.app.domain.model.DesafioHecho
import com.desconectado.app.domain.model.AchievementCriterion
import com.desconectado.app.domain.model.AchievementDefinition
import com.desconectado.app.domain.model.AchievementProgress
import com.desconectado.app.domain.model.CosmeticPreferences
import com.desconectado.app.domain.model.Perfil
import com.desconectado.app.domain.model.UpcomingPointExpiry
import com.desconectado.app.domain.model.TipoRecompensa
import com.desconectado.app.domain.model.UserPreferences
import com.desconectado.app.domain.ProgressMetrics
import com.desconectado.app.ui.profile.DesafiosHechosUiState
import com.desconectado.app.ui.profile.PerfilScreen
import com.desconectado.app.ui.profile.PerfilUiState
import com.desconectado.app.ui.profile.ProximoVencimientoUiState
import com.desconectado.app.ui.profile.ProgresoUiState
import com.desconectado.app.ui.profile.EdicionNombreUiState
import com.desconectado.app.ui.profile.PreferenciasPerfilUiState
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
        proximo: ProximoVencimientoUiState = ProximoVencimientoUiState.SinVencimientos,
        progreso: ProgresoUiState = ProgresoUiState.Oculto,
        preferenciasCosmeticas: CosmeticPreferences = CosmeticPreferences(),
        edicionNombre: EdicionNombreUiState = EdicionNombreUiState(),
        onEditarNombre: () -> Unit = {},
        onCambiarNombre: (String) -> Unit = {},
        onGuardarNombre: () -> Unit = {},
        onCancelarEdicionNombre: () -> Unit = {},
        preferenciasUsuario: PreferenciasPerfilUiState = PreferenciasPerfilUiState.Oculto,
        onGuardarMetaSemanal: (Int) -> Unit = {},
        onConfigurarNotificaciones: (Boolean) -> Unit = {},
        onAbrirAjustesPrivacidad: () -> Unit = {},
    ) = compose.setContent {
        DesConectadoTheme {
            PerfilScreen(
                estado = estado,
                onReintentar = {},
                onCerrarSesion = onCerrarSesion,
                desafiosHechos = desafios,
                onReintentarPuntos = onReintentarPuntos,
                proximoVencimiento = proximo,
                progreso = progreso,
                preferenciasCosmeticas = preferenciasCosmeticas,
                edicionNombre = edicionNombre,
                onEditarNombre = onEditarNombre,
                onCambiarNombre = onCambiarNombre,
                onGuardarNombre = onGuardarNombre,
                onCancelarEdicionNombre = onCancelarEdicionNombre,
                preferenciasUsuario = preferenciasUsuario,
                onGuardarMetaSemanal = onGuardarMetaSemanal,
                onConfigurarNotificaciones = onConfigurarNotificaciones,
                onAbrirAjustesPrivacidad = onAbrirAjustesPrivacidad,
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
    fun muestraPuntosYDiasDelProximoVencimientoDebajoDelSaldo() {
        val vencimiento = UpcomingPointExpiry(35, Instant.parse("2026-11-04T07:00:00Z"), 29)
        mostrar(proximo = ProximoVencimientoUiState.Proximo(vencimiento))

        compose.onNodeWithTag("puntos_perfil").assertIsDisplayed()
        compose.onNodeWithTag("proximo_vencimiento")
            .assertIsDisplayed()
            .assertTextEquals("35 puntos están por vencer en 29 días.")
        assertTrue(arriba("proximo_vencimiento") > arriba("puntos_perfil") && arriba("proximo_vencimiento") < arriba("Historial de desafíos"))
    }

    @Test
    fun muestraMetricasProgresoYLogrosSinInventarMetaSemanal() {
        val primerPaso = AchievementProgress(
            AchievementDefinition(
                id = "primer-paso",
                name = "Primer paso",
                description = "Completa tu primer desafío.",
                criterion = AchievementCriterion.COMPLETED_CHALLENGES,
                threshold = 1,
                order = 1,
            ),
            progress = 1,
        )
        mostrar(progreso = ProgresoUiState.Datos(
            metricas = ProgressMetrics(1, 1_800, 1, 1_800, null, emptyMap()),
            logros = listOf(primerPaso),
        ))

        compose.onNodeWithTag("resumen_progreso").assertIsDisplayed()
        compose.onNodeWithText("1 desafío completado").assertIsDisplayed()
        compose.onNodeWithText("Meta semanal sin configurar.").assertIsDisplayed()
        compose.onNodeWithTag("logro_primer-paso").assertIsDisplayed()
        compose.onNodeWithText("Desbloqueado").assertIsDisplayed()
    }

    @Test
    fun aplicaMarcoDePerfilYEstrellaEspecialDePuntos() {
        mostrar(preferenciasCosmeticas = CosmeticPreferences(mapOf(
            TipoRecompensa.MARCO_PERFIL to "profile-frame-naturaleza",
            TipoRecompensa.ICONO_PUNTOS to "point-icon-estrella-especial",
        )))

        compose.onNodeWithTag("avatar_perfil_enmarcado").assertIsDisplayed()
        compose.onNodeWithTag("icono_puntos_cosmetico").assertIsDisplayed()
        compose.onNodeWithTag("puntos_perfil").assertIsDisplayed()
    }

    @Test
    fun permiteAbrirEdicionDeNombreVisibleSinCambiarIdentidad() {
        mostrar(edicionNombre = EdicionNombreUiState(editando = true, texto = "Ana Prueba"))

        compose.onNodeWithTag("campo_editar_username").assertIsDisplayed()
        compose.onNodeWithTag("boton_guardar_nombre").assertIsDisplayed()
        compose.onNodeWithTag("texto_email").assertTextEquals("ana@mail.com")
    }

    @Test
    fun permiteElegirMetaSinDefaultYConfigurarNotificaciones() {
        var metaGuardada = 0
        var notificaciones = false
        mostrar(
            preferenciasUsuario = PreferenciasPerfilUiState.Datos(UserPreferences()),
            onGuardarMetaSemanal = { metaGuardada = it },
            onConfigurarNotificaciones = { notificaciones = it },
        )

        compose.onNodeWithText("Elegir meta semanal").assertIsDisplayed()
        compose.onNodeWithTag("boton_guardar_meta").assertIsNotEnabled()
        compose.onNodeWithTag("meta_semanal_selector").performClick()
        compose.onNodeWithText("120 min/semana").performClick()
        compose.onNodeWithTag("boton_guardar_meta").performClick()
        compose.onNodeWithTag("toggle_notificaciones").performClick()

        assertEquals(120, metaGuardada)
        assertEquals(true, notificaciones)
    }

    @Test
    fun explicaUsoDeEstadisticasYOfreceAbrirAjustesDeAndroid() {
        mostrar()

        compose.onNodeWithText("Usamos las estadísticas de uso del dispositivo para medir tus desafíos.", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("boton_ajustes_privacidad").assertIsDisplayed()
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
    fun noExisteNingunaAccionParaGanarOGastarPuntosDesdeElPerfil() {
        mostrar(
            estado = PerfilUiState.Datos(perfil.copy(puntos = 60)),
            desafios = DesafiosHechosUiState.Lista(desafiosHechos),
        )

        // Las acciones editan perfil/configuración; ninguna altera puntos.
        compose.onAllNodes(hasClickAction()).assertCountEquals(3)
        compose.onNodeWithTag("boton_cerrar_sesion").assertIsDisplayed()
        compose.onNodeWithTag("boton_editar_nombre").assertIsDisplayed()
        compose.onNodeWithTag("boton_ajustes_privacidad").assertIsDisplayed()
    }
}
