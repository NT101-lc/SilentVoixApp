package com.silentvoix.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Visual direction: fired clay, honey and dó paper. Terracotta is the one action colour (start,
// favourite, selected tab); honey marks "in between" states; sage says "ready / working".
// Neutrals are warm all the way down: cream paper in light, cacao in dark, never grey or blue.
// Text/background pairs are kept at or above WCAG AA.

internal val LightColors = lightColorScheme(
    primary = Color(0xFFA64B2A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBCC),
    onPrimaryContainer = Color(0xFF3A0B00),
    secondary = Color(0xFF7A5900),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFE3A6),
    onSecondaryContainer = Color(0xFF261A00),
    tertiary = Color(0xFF52652F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDDEBBB),
    onTertiaryContainer = Color(0xFF131F00),
    error = Color(0xFFA3213A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFD9DC),
    onErrorContainer = Color(0xFF40000E),
    background = Color(0xFFFBF6EE),
    onBackground = Color(0xFF2B1F1A),
    surface = Color(0xFFFBF6EE),
    onSurface = Color(0xFF2B1F1A),
    surfaceVariant = Color(0xFFEFDFD2),
    onSurfaceVariant = Color(0xFF594840),
    outline = Color(0xFF87746A),
    outlineVariant = Color(0xFFDBCABC),
    inverseSurface = Color(0xFF33271F),
    inverseOnSurface = Color(0xFFFBEEE4),
    inversePrimary = Color(0xFFFFB596),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6EEE3),
    surfaceContainer = Color(0xFFF0E6D9),
    surfaceContainerHigh = Color(0xFFEADFD0),
    surfaceContainerHighest = Color(0xFFE3D6C5),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB596),
    onPrimary = Color(0xFF5B1B04),
    primaryContainer = Color(0xFF7D3316),
    onPrimaryContainer = Color(0xFFFFDBCC),
    secondary = Color(0xFFEDC268),
    onSecondary = Color(0xFF402D00),
    secondaryContainer = Color(0xFF5C4300),
    onSecondaryContainer = Color(0xFFFFE3A6),
    tertiary = Color(0xFFBDCE98),
    onTertiary = Color(0xFF273511),
    tertiaryContainer = Color(0xFF3D4C24),
    onTertiaryContainer = Color(0xFFDDEBBB),
    error = Color(0xFFFFB2B7),
    onError = Color(0xFF670020),
    errorContainer = Color(0xFF8A0F2C),
    onErrorContainer = Color(0xFFFFD9DC),
    background = Color(0xFF1B1512),
    onBackground = Color(0xFFF2E6DC),
    surface = Color(0xFF1B1512),
    onSurface = Color(0xFFF2E6DC),
    surfaceVariant = Color(0xFF52443B),
    onSurfaceVariant = Color(0xFFD8C3B6),
    outline = Color(0xFFA28E82),
    outlineVariant = Color(0xFF4A3D35),
    inverseSurface = Color(0xFFF2E6DC),
    inverseOnSurface = Color(0xFF382C25),
    inversePrimary = Color(0xFFA64B2A),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF4A3E36),
    surfaceContainerLowest = Color(0xFF15100D),
    surfaceContainerLow = Color(0xFF231C18),
    surfaceContainer = Color(0xFF29211C),
    surfaceContainerHigh = Color(0xFF342B25),
    surfaceContainerHighest = Color(0xFF40352E),
)

/**
 * The home screen's hero card: fired clay in both themes, so the app's one "brand moment" looks
 * the same whether the page around it is paper or cacao. Text pairs are at or above WCAG AA.
 */
object HeroPalette {
    val ClayTop = Color(0xFFA64B2A)
    val ClayBottom = Color(0xFF7B3017)
    val OnClay = Color(0xFFFFF6EC)
    val OnClayMuted = Color(0xFFFFDFCF)
    val Ring = Color(0xFFFFC078)
}

/**
 * Colours for the gesture capture stage. It stays cacao-dark in both themes so the framing area
 * reads as the focus of the app rather than as another surface; its light is lantern amber.
 */
object StagePalette {
    val Ink = Color(0xFF1A1310)
    /** Hairline around the stage so it separates from a dark page. */
    val InkEdge = Color(0xFF3F3028)
    /** Lantern glow behind the framing guide while the camera is off. */
    val Glow = Color(0xFF5A3420)
    /** Bottom scrim behind live captions, over the camera image. */
    val CaptionScrim = Color(0xE61A1310)
    val Guide = Color(0xFFFFC078)
    val GuideIdle = Color(0xFF9A887C)
    val OnInk = Color(0xFFF7EDE3)
    val OnInkMuted = Color(0xFFC6B3A6)
    /** Error tone on ink (camera/permission failures); 10.8:1 on [Ink]. */
    val Error = Color(0xFFFFB4A8)
    val ErrorSoft = Color(0x33FF8A70)
}
