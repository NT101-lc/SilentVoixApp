package com.silentvoix.app.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Composable
fun SilentVoixTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Dark status-bar icons when true; defaults to following the theme. Set by screens painted under the bar. */
    lightStatusBars: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    // Keep system bar icons readable when the in-app theme differs from the system one.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = lightStatusBars ?: !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = (if (darkTheme) DarkColors else LightColors).animated(),
        typography = SilentVoixTypography,
        shapes = SilentVoixShapes,
        content = content,
    )
}

/** Eases every colour to its new value, so switching theme is a soft change of light, not a flash. */
@Composable
private fun ColorScheme.animated(): ColorScheme {
    @Composable
    fun Color.eased(): Color = animateColorAsState(this, tween(durationMillis = 350), label = "scheme").value
    return copy(
        primary = primary.eased(),
        onPrimary = onPrimary.eased(),
        primaryContainer = primaryContainer.eased(),
        onPrimaryContainer = onPrimaryContainer.eased(),
        secondary = secondary.eased(),
        onSecondary = onSecondary.eased(),
        secondaryContainer = secondaryContainer.eased(),
        onSecondaryContainer = onSecondaryContainer.eased(),
        tertiary = tertiary.eased(),
        onTertiary = onTertiary.eased(),
        tertiaryContainer = tertiaryContainer.eased(),
        onTertiaryContainer = onTertiaryContainer.eased(),
        error = error.eased(),
        onError = onError.eased(),
        errorContainer = errorContainer.eased(),
        onErrorContainer = onErrorContainer.eased(),
        background = background.eased(),
        onBackground = onBackground.eased(),
        surface = surface.eased(),
        onSurface = onSurface.eased(),
        surfaceVariant = surfaceVariant.eased(),
        onSurfaceVariant = onSurfaceVariant.eased(),
        outline = outline.eased(),
        outlineVariant = outlineVariant.eased(),
        inverseSurface = inverseSurface.eased(),
        inverseOnSurface = inverseOnSurface.eased(),
        inversePrimary = inversePrimary.eased(),
        surfaceBright = surfaceBright.eased(),
        surfaceContainerLowest = surfaceContainerLowest.eased(),
        surfaceContainerLow = surfaceContainerLow.eased(),
        surfaceContainer = surfaceContainer.eased(),
        surfaceContainerHigh = surfaceContainerHigh.eased(),
        surfaceContainerHighest = surfaceContainerHighest.eased(),
        surfaceTint = primary.eased(),
    )
}
