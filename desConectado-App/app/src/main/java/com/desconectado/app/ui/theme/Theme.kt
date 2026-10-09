package com.desconectado.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
    primary = VerdeMenta,
    onPrimary = VerdeSobreMenta,
    primaryContainer = VerdeCabecera,
    onPrimaryContainer = VerdeSobreCabecera,
    secondary = VerdeMentaSuave,
    onSecondary = VerdeSobreMenta,
    secondaryContainer = VerdePildora,
    onSecondaryContainer = VerdeMentaSuave,
    background = NeutroFondoOscuro,
    onBackground = NeutroTextoOscuro,
    surface = NeutroFondoOscuro,
    onSurface = NeutroTextoOscuro,
    surfaceVariant = VerdeTarjetaOscura,
    onSurfaceVariant = VerdeTextoSecundario,
    outline = VerdeTextoSecundario,
    error = ErrorOscuro,
)

/** Tema Material 3 de la app. El diseño de referencia es oscuro, por eso es el predeterminado. */
@Composable
fun DesConectadoTheme(
    oscuro: Boolean = true,
    temaId: String? = null,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = esquemaCosmetico(temaId, oscuro),
        typography = Tipografia,
        content = content,
    )
}

private fun esquemaCosmetico(temaId: String?, oscuro: Boolean): ColorScheme = when (temaId) {
    "theme-bosque" -> if (oscuro) darkColorScheme(
        primary = Color(0xFF78C69A), onPrimary = Color(0xFF062D1D),
        secondary = Color(0xFF8DC9D8), background = Color(0xFF101A16),
        surface = Color(0xFF101A16), onSurface = Color(0xFFE3EEE7),
    ) else EsquemaClaro
    "theme-atardecer", "debug-theme-1-punto" -> if (oscuro) darkColorScheme(
        primary = Color(0xFFFFB49A), onPrimary = Color(0xFF4D1609),
        secondary = Color(0xFFFFB0C0), background = Color(0xFF211411),
        surface = Color(0xFF211411), onSurface = Color(0xFFFFEDE6),
    ) else lightColorScheme(
        primary = Color(0xFFB84227), onPrimary = Color.White,
        secondary = Color(0xFF9C4160), background = Color(0xFFFFF7F2),
        surface = Color(0xFFFFF7F2), onSurface = Color(0xFF241714),
    )
    "theme-oceano" -> if (oscuro) darkColorScheme(
        primary = Color(0xFF75D0D4), onPrimary = Color(0xFF00363C),
        secondary = Color(0xFF9DC6FF), background = Color(0xFF0D1C25),
        surface = Color(0xFF0D1C25), onSurface = Color(0xFFE1F3F5),
    ) else lightColorScheme(
        primary = Color(0xFF006976), onPrimary = Color.White,
        secondary = Color(0xFF315E92), background = Color(0xFFF3FAFB),
        surface = Color(0xFFF3FAFB), onSurface = Color(0xFF111E22),
    )
    "theme-noche" -> darkColorScheme(
        primary = Color(0xFFA8C7FF), onPrimary = Color(0xFF18345B),
        secondary = Color(0xFF8DD7D2), background = Color(0xFF0D1726),
        surface = Color(0xFF0D1726), onSurface = Color(0xFFE3ECF8),
    )
    else -> if (oscuro) EsquemaOscuro else EsquemaClaro
}
