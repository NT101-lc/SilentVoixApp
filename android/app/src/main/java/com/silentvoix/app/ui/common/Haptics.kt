package com.silentvoix.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * A tap-confirmation buzz, gated on the user's setting. Uses Compose's own haptic feedback,
 * which needs no permission.
 */
@Composable
fun rememberHapticTap(enabled: Boolean): () -> Unit {
    val haptics = LocalHapticFeedback.current
    return remember(enabled, haptics) {
        {
            if (enabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
}
