package com.silentvoix.app.ui.common

import android.provider.Settings
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext

/** Whether decorative motion should play, given the system's animator duration scale. */
fun shouldAnimate(animatorDurationScale: Float): Boolean = animatorDurationScale > 0f

/**
 * False when the user has turned animations off (Developer options or accessibility "Remove
 * animations"); ambient scene motion then stands still.
 */
@Composable
fun rememberShouldAnimate(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        shouldAnimate(Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f))
    }
}

/** Sinks a little while pressed and springs back, so a card feels like something you touch. */
@Composable
fun Modifier.pressBounce(interactionSource: InteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "pressBounce",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
