package com.desconectado.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.desconectado.app.ui.profile.TutorialPermisoUsoDialog
import com.desconectado.app.ui.theme.DesConectadoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TutorialPermisoUsoDialogTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun recorreLosTresPasosYAbreLosAjustesIndicados() {
        var aperturasUso = 0
        var aperturasApp = 0
        compose.setContent {
            DesConectadoTheme {
                TutorialPermisoUsoDialog(
                    onDismiss = {},
                    onAbrirPermisosUso = { aperturasUso++ },
                    onAbrirInformacionApp = { aperturasApp++ },
                    tienePermisoUso = { false },
                )
            }
        }

        compose.onNodeWithText("Paso 1 de 3").assertIsDisplayed()
        compose.onNodeWithTag("tutorial_permiso_accion_principal").performClick()
        compose.onNodeWithText("Paso 2 de 3").assertIsDisplayed()
        compose.onNodeWithTag("tutorial_permiso_accion_principal").performClick()
        compose.onNodeWithText("Paso 3 de 3").assertIsDisplayed()
        compose.onNodeWithTag("tutorial_permiso_abrir_ajustes_final").performClick()

        assertEquals(2, aperturasUso)
        assertEquals(1, aperturasApp)
    }

    @Test
    fun exigePermisoAntesDeCerrarLaGuiaYPermiteFinalizarAlConcederlo() {
        var permitido = false
        var finalizaciones = 0
        compose.setContent {
            DesConectadoTheme {
                TutorialPermisoUsoDialog(
                    onDismiss = { finalizaciones++ },
                    onAbrirPermisosUso = {},
                    onAbrirInformacionApp = {},
                    tienePermisoUso = { permitido },
                )
            }
        }

        repeat(2) { compose.onNodeWithTag("tutorial_permiso_accion_principal").performClick() }
        compose.onNodeWithTag("tutorial_permiso_accion_principal").performClick()
        compose.onNodeWithTag("tutorial_permiso_error").assertIsDisplayed()
        assertEquals(0, finalizaciones)

        permitido = true
        compose.onNodeWithTag("tutorial_permiso_accion_principal").performClick()
        assertEquals(1, finalizaciones)
    }
}