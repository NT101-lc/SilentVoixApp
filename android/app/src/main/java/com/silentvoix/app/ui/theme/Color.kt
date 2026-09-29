package com.silentvoix.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Visual direction: a deep ink-navy base, warm paper neutrals, and a single mint accent.
// The accent is reserved for the primary action and live state so it keeps its meaning.
// Text/background pairs are kept at or above WCAG AA.

internal val LightColors = lightColorScheme(
    primary = Color(0xFF0A6255),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA8F3E1),
    onPrimaryContainer = Color(0xFF00201B),
    secondary = Color(0xFF5C4B31),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF0E1C8),
    onSecondaryContainer = Color(0xFF1F1708),
    tertiary = Color(0xFF3F4F8F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDDE1FF),
    onTertiaryContainer = Color(0xFF00164C),
    error = Color(0xFFA4232A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD7),
    onErrorContainer = Color(0xFF410004),
    background = Color(0xFFF7F4EF),
    onBackground = Color(0xFF12161D),
    surface = Color(0xFFF7F4EF),
    onSurface = Color(0xFF12161D),
    surfaceVariant = Color(0xFFE6E0D6),
    onSurfaceVariant = Color(0xFF474F5A),
    outline = Color(0xFF727A85),
    outlineVariant = Color(0xFFCFC9BE),
    inverseSurface = Color(0xFF151C28),
    inverseOnSurface = Color(0xFFECF0F6),
    inversePrimary = Color(0xFF5FE3C8),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2EFE8),
    surfaceContainer = Color(0xFFEDE8DF),
    surfaceContainerHigh = Color(0xFFE6E0D5),
    surfaceContainerHighest = Color(0xFFDFD8CB),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFF5FE3C8),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF0A5046),
    onPrimaryContainer = Color(0xFFA8F3E1),
    secondary = Color(0xFFDCC5A2),
    onSecondary = Color(0xFF3B2E18),
    secondaryContainer = Color(0xFF52432C),
    onSecondaryContainer = Color(0xFFF0E1C8),
    tertiary = Color(0xFFB6C1FF),
    onTertiary = Color(0xFF1D2C63),
    tertiaryContainer = Color(0xFF344176),
    onTertiaryContainer = Color(0xFFDDE1FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD7),
    background = Color(0xFF0E1522),
    onBackground = Color(0xFFE7EBF2),
    surface = Color(0xFF0E1522),
    onSurface = Color(0xFFE7EBF2),
    surfaceVariant = Color(0xFF3E4756),
    onSurfaceVariant = Color(0xFFB2BCCB),
    outline = Color(0xFF7C8796),
    outlineVariant = Color(0xFF2A3342),
    inverseSurface = Color(0xFFE7EBF2),
    inverseOnSurface = Color(0xFF1A202B),
    inversePrimary = Color(0xFF0A6255),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF344156),
    surfaceContainerLowest = Color(0xFF090E17),
    surfaceContainerLow = Color(0xFF151D2B),
    surfaceContainer = Color(0xFF1A2331),
    surfaceContainerHigh = Color(0xFF212B3A),
    surfaceContainerHighest = Color(0xFF2A3545),
)

/**
 * Colours for the gesture capture stage. It stays ink-dark in both themes so the framing area
 * reads as the focus of the app rather than as another surface.
 */
object StagePalette {
    val Ink = Color(0xFF070B13)
    /** Hairline around the stage so it separates from a dark page. */
    val InkEdge = Color(0xFF223047)
    /** Bottom scrim behind live captions, over the camera image. */
    val CaptionScrim = Color(0xE6070B13)
    val Guide = Color(0xFF5FE3C8)
    val GuideIdle = Color(0xFF64748B)
    val OnInk = Color(0xFFE7EBF2)
    val OnInkMuted = Color(0xFF94A3B8)
    /** Error tone on ink (camera/permission failures); 7.9:1 on [Ink]. */
    val Error = Color(0xFFFFB4AB)
    val ErrorSoft = Color(0x33FF8A80)
}
