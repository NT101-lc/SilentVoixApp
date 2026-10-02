package com.silentvoix.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Visual direction: a hand-painted countryside, after the look of Japanese animated films.
// Day is a meadow at noon: forest-green actions, a clear sky blue, a red-tile accent, all on warm
// paper. Night is a field of fireflies: a deep blue sky, new-leaf green, lantern orange.
// Green is the one action colour; sky blue and tile red are supporting accents, never actions.
// Every text/background pair is checked against WCAG AA by PaletteContrastTest.

internal val LightColors = lightColorScheme(
    primary = Color(0xFF35632E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCDEBB4),
    onPrimaryContainer = Color(0xFF0E2206),
    secondary = Color(0xFF2B6488),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFE7F6),
    onSecondaryContainer = Color(0xFF001E2E),
    tertiary = Color(0xFFA9432B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDBCF),
    onTertiaryContainer = Color(0xFF3A0A00),
    error = Color(0xFFA11F3B),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFD9DE),
    onErrorContainer = Color(0xFF400011),
    background = Color(0xFFFBF6E8),
    onBackground = Color(0xFF22301F),
    surface = Color(0xFFFBF6E8),
    onSurface = Color(0xFF22301F),
    surfaceVariant = Color(0xFFE3E5D0),
    onSurfaceVariant = Color(0xFF4B5744),
    outline = Color(0xFF6C7864),
    outlineVariant = Color(0xFFD2D4BE),
    inverseSurface = Color(0xFF2E3A2B),
    inverseOnSurface = Color(0xFFF1F4E6),
    inversePrimary = Color(0xFFA9D88C),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFFDF5),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4EFDE),
    surfaceContainer = Color(0xFFEEE8D5),
    surfaceContainerHigh = Color(0xFFE7E1CB),
    surfaceContainerHighest = Color(0xFFE0D9C1),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFFA9D88C),
    onPrimary = Color(0xFF14380A),
    primaryContainer = Color(0xFF2D5122),
    onPrimaryContainer = Color(0xFFC5F2A6),
    secondary = Color(0xFF9DCDEE),
    onSecondary = Color(0xFF003350),
    secondaryContainer = Color(0xFF164B6C),
    onSecondaryContainer = Color(0xFFCFE7F6),
    tertiary = Color(0xFFFFB592),
    onTertiary = Color(0xFF561F05),
    tertiaryContainer = Color(0xFF743318),
    onTertiaryContainer = Color(0xFFFFDBCF),
    error = Color(0xFFFFB3BA),
    onError = Color(0xFF670020),
    errorContainer = Color(0xFF8C1231),
    onErrorContainer = Color(0xFFFFD9DE),
    background = Color(0xFF111A27),
    onBackground = Color(0xFFE6ECF0),
    surface = Color(0xFF111A27),
    onSurface = Color(0xFFE6ECF0),
    surfaceVariant = Color(0xFF364253),
    onSurfaceVariant = Color(0xFFBCC7D1),
    outline = Color(0xFF8994A0),
    outlineVariant = Color(0xFF313D4E),
    inverseSurface = Color(0xFFE6ECF0),
    inverseOnSurface = Color(0xFF253041),
    inversePrimary = Color(0xFF35632E),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF3A4759),
    surfaceContainerLowest = Color(0xFF0B121D),
    surfaceContainerLow = Color(0xFF18212F),
    surfaceContainer = Color(0xFF1D2735),
    surfaceContainerHigh = Color(0xFF263142),
    surfaceContainerHighest = Color(0xFF303C4E),
)

/**
 * The home screen's hero card: a forest at dusk in both themes, with firefly-light rings, so the
 * app's one "brand moment" looks the same on a day or a night page. Text pairs meet WCAG AA.
 */
object HeroPalette {
    val Top = Color(0xFF2F5A2A)
    val Bottom = Color(0xFF16301C)
    val OnHero = Color(0xFFFBF6E8)
    val OnHeroMuted = Color(0xFFDCE8CE)
    val Ring = Color(0xFFE4F59E)
}

/**
 * Colours for the gesture capture stage: a night sky in both themes, so the framing area reads as
 * the focus of the app rather than another surface. Its light is firefly green.
 */
object StagePalette {
    val Ink = Color(0xFF0F1826)
    /** Hairline around the stage so it separates from a dark page. */
    val InkEdge = Color(0xFF2B3A52)
    /** Moonlight pooled behind the framing guide while the camera is off. */
    val Glow = Color(0xFF29436A)
    /** Bottom scrim behind live captions, over the camera image. */
    val CaptionScrim = Color(0xE60F1826)
    val Guide = Color(0xFFE4F59E)
    val GuideIdle = Color(0xFF7F8EA4)
    val OnInk = Color(0xFFEEF2F6)
    val OnInkMuted = Color(0xFFAEBACA)
    /** Error tone on the stage (camera/permission failures). */
    val Error = Color(0xFFFFB4A8)
    val ErrorSoft = Color(0x33FF8A70)
}
