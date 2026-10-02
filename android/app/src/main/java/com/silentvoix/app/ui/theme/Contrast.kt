package com.silentvoix.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * WCAG 2.1 contrast ratio between two opaque colours, from 1 (identical) to 21 (black on white).
 * Text needs 4.5, icons and large marks 3.
 */
fun contrastRatio(a: Color, b: Color): Float {
    val lighter = maxOf(a.luminance(), b.luminance())
    val darker = minOf(a.luminance(), b.luminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}
