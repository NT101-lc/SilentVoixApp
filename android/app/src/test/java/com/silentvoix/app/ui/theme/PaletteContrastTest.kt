package com.silentvoix.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every text/background pair the screens use, in both themes, measured against WCAG 2.1:
 * 4.5:1 for text, 3:1 for icons, outlines and large decorative marks.
 */
class PaletteContrastTest {

    private val text = 4.5f
    private val graphic = 3f

    private fun assertContrast(name: String, foreground: Color, background: Color, minimum: Float) {
        val ratio = contrastRatio(foreground, background)
        assertTrue("$name: ${"%.2f".format(ratio)}:1 is below $minimum:1", ratio >= minimum)
    }

    private fun schemePairs(scheme: ColorScheme): List<Triple<String, Pair<Color, Color>, Float>> = with(scheme) {
        val surfaces = listOf(
            "background" to background,
            "surfaceContainerLowest" to surfaceContainerLowest,
            "surfaceContainerLow" to surfaceContainerLow,
            "surfaceContainer" to surfaceContainer,
            "surfaceContainerHigh" to surfaceContainerHigh,
            "surfaceContainerHighest" to surfaceContainerHighest,
        )
        listOf(
            Triple("onPrimary/primary", onPrimary to primary, text),
            Triple("onPrimaryContainer/primaryContainer", onPrimaryContainer to primaryContainer, text),
            Triple("onSecondary/secondary", onSecondary to secondary, text),
            Triple("onSecondaryContainer/secondaryContainer", onSecondaryContainer to secondaryContainer, text),
            Triple("onTertiary/tertiary", onTertiary to tertiary, text),
            Triple("onTertiaryContainer/tertiaryContainer", onTertiaryContainer to tertiaryContainer, text),
            Triple("onError/error", onError to error, text),
            Triple("onErrorContainer/errorContainer", onErrorContainer to errorContainer, text),
            Triple("inverseOnSurface/inverseSurface", inverseOnSurface to inverseSurface, text),
        ) + surfaces.flatMap { (name, surface) ->
            listOf(
                Triple("onSurface/$name", onSurface to surface, text),
                Triple("onSurfaceVariant/$name", onSurfaceVariant to surface, text),
                // Text buttons, links, selected labels and error text sit straight on surfaces.
                Triple("primary/$name", primary to surface, text),
                Triple("error/$name", error to surface, text),
                Triple("outline/$name", outline to surface, graphic),
            )
        }
    }

    @Test
    fun `the light scheme meets WCAG AA everywhere`() {
        schemePairs(LightColors).forEach { (name, pair, minimum) -> assertContrast("light $name", pair.first, pair.second, minimum) }
    }

    @Test
    fun `the dark scheme meets WCAG AA everywhere`() {
        schemePairs(DarkColors).forEach { (name, pair, minimum) -> assertContrast("dark $name", pair.first, pair.second, minimum) }
    }

    @Test
    fun `the camera stage is readable over its night sky`() {
        assertContrast("stage text", StagePalette.OnInk, StagePalette.Ink, 7f)
        assertContrast("stage muted text", StagePalette.OnInkMuted, StagePalette.Ink, text)
        assertContrast("stage error", StagePalette.Error, StagePalette.Ink, text)
        assertContrast("stage guide", StagePalette.Guide, StagePalette.Ink, graphic)
        assertContrast("stage idle guide", StagePalette.GuideIdle, StagePalette.Ink, graphic)
    }

    @Test
    fun `the ratio matches the WCAG reference values`() {
        assertEquals(21f, contrastRatio(Color.Black, Color.White), 0.01f)
        assertEquals(1f, contrastRatio(Color(0xFF777777), Color(0xFF777777)), 0.01f)
        // #767676 on white is the classic "just passes AA" grey.
        assertEquals(4.54f, contrastRatio(Color(0xFF767676), Color.White), 0.01f)
    }

    @Test
    fun `the palettes are the meadow day and firefly night, not the old clay`() {
        // Day: green actions on warm paper. Night: a deep blue sky, not brown.
        with(LightColors.primary) { assertTrue("light primary should be green", green > red && green > blue) }
        with(LightColors.background) { assertTrue("light background should be warm paper", red >= blue) }
        with(DarkColors.background) { assertTrue("dark background should be night blue", blue > red) }
        with(StagePalette.Ink) { assertTrue("stage should be night blue", blue > red) }
    }
}
