package com.desconectado.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val EsquemaClaro = lightColorScheme(
    primary = VerdePrimario,
    onPrimary = VerdeSobrePrimario,
    primaryContainer = VerdePrimarioClaro,
    secondary = AzulSecundario,
    onSecondary = VerdeSobrePrimario,
    secondaryContainer = AzulSecundarioClaro,
    background = NeutroFondoClaro,
    onBackground = NeutroTextoClaro,
    surface = NeutroFondoClaro,
    onSurface = NeutroTextoClaro,
    error = ErrorClaro,
)

private val EsquemaOscuro = darkColorScheme(
    primary = VerdePrimarioOscuro,
    onPrimary = VerdeContenedorOscuro,
    primaryContainer = VerdeContenedorOscuro,
    secondary = AzulSecundarioOscuro,
    onSecondary = AzulContenedorOscuro,
    secondaryContainer = AzulContenedorOscuro,
    background = NeutroFondoOscuro,
    onBackground = NeutroTextoOscuro,
    surface = NeutroFondoOscuro,
    onSurface = NeutroTextoOscuro,
    error = ErrorOscuro,
)

/** Tema Material 3 de la app: claro u oscuro según el sistema. */
@Composable
fun DesConectadoTheme(
    oscuro: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (oscuro) EsquemaOscuro else EsquemaClaro,
        typography = Tipografia,
        content = content,
    )
}
