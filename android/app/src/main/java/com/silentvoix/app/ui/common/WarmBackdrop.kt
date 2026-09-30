package com.silentvoix.app.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Two soft pools of warm light behind the top of a screen, clay from the left and honey from the
 * right, fading into the page. Decoration only: text over it keeps the page's own contrast.
 */
@Composable
fun Modifier.warmBackdrop(): Modifier {
    val clay = MaterialTheme.colorScheme.primaryContainer
    val honey = MaterialTheme.colorScheme.secondaryContainer
    return drawBehind {
        drawRect(
            Brush.radialGradient(
                colors = listOf(clay.copy(alpha = 0.55f), Color.Transparent),
                center = Offset(size.width * 0.05f, 0f),
                radius = size.width * 0.95f,
            ),
        )
        drawRect(
            Brush.radialGradient(
                colors = listOf(honey.copy(alpha = 0.45f), Color.Transparent),
                center = Offset(size.width * 1.05f, size.width * 0.1f),
                radius = size.width * 0.8f,
            ),
        )
    }
}
