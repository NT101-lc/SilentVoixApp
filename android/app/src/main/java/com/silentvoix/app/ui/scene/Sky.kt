package com.silentvoix.app.ui.scene

import androidx.compose.ui.graphics.Color

/** The part of the day a scene is painted for. */
enum class TimeOfDay {
    DAWN, DAY, DUSK, NIGHT;

    companion object {
        /** [hour] is 0–23 on the device's clock. */
        fun from(hour: Int): TimeOfDay {
            require(hour in 0..23) { "hour must be 0..23, was $hour" }
            return when (hour) {
                in 5..7 -> DAWN
                in 8..16 -> DAY
                in 17..18 -> DUSK
                else -> NIGHT
            }
        }
    }
}

/**
 * Colours of a painted countryside at one time of day. [onSky] is the text colour for anything
 * written over the upper sky (between [skyTop] and [skyMiddle]); [isLight] says whether the system
 * bar icons over it should be dark. [hills] go from the farthest to the nearest.
 */
data class SkyPalette(
    val skyTop: Color,
    val skyMiddle: Color,
    val skyHorizon: Color,
    val onSky: Color,
    val isLight: Boolean,
    val showSun: Boolean,
    val sun: Color,
    val sunGlow: Color,
    val starAlpha: Float,
    val fireflies: Boolean,
    val cloudLight: Color,
    val cloudShadow: Color,
    val hills: List<Color>,
)

fun skyPalette(time: TimeOfDay): SkyPalette = when (time) {
    TimeOfDay.DAWN -> SkyPalette(
        skyTop = Color(0xFFF2B9A1),
        skyMiddle = Color(0xFFF7CFB0),
        skyHorizon = Color(0xFFFCEBC9),
        onSky = Color(0xFF3A1A10),
        isLight = true,
        showSun = true,
        sun = Color(0xFFFFE3A8),
        sunGlow = Color(0xFFFFC98A),
        starAlpha = 0f,
        fireflies = false,
        cloudLight = Color(0xFFFFF3E8),
        cloudShadow = Color(0xFFEBB4A6),
        hills = listOf(Color(0xFFBFC48F), Color(0xFF8AA36B), Color(0xFF587640)),
    )
    TimeOfDay.DAY -> SkyPalette(
        skyTop = Color(0xFF7FBEEA),
        skyMiddle = Color(0xFFB6DCF4),
        skyHorizon = Color(0xFFEAF4E6),
        onSky = Color(0xFF10304A),
        isLight = true,
        showSun = true,
        sun = Color(0xFFFFF6D6),
        sunGlow = Color(0xFFFFF1B8),
        starAlpha = 0f,
        fireflies = false,
        cloudLight = Color(0xFFFFFFFF),
        cloudShadow = Color(0xFFC8DCEC),
        hills = listOf(Color(0xFFA3CC8C), Color(0xFF71A65D), Color(0xFF3F7535)),
    )
    TimeOfDay.DUSK -> SkyPalette(
        skyTop = Color(0xFF3D3A78),
        skyMiddle = Color(0xFF6E4B80),
        skyHorizon = Color(0xFFF0A06C),
        onSky = Color(0xFFFFF4E6),
        isLight = false,
        showSun = true,
        sun = Color(0xFFFFC27A),
        sunGlow = Color(0xFFFF9A62),
        starAlpha = 0.35f,
        fireflies = true,
        cloudLight = Color(0xFFF7C2A2),
        cloudShadow = Color(0xFF8D6A8F),
        hills = listOf(Color(0xFF7E6E8C), Color(0xFF4F5A5E), Color(0xFF2C3A33)),
    )
    TimeOfDay.NIGHT -> SkyPalette(
        skyTop = Color(0xFF0E1A33),
        skyMiddle = Color(0xFF1B2D52),
        skyHorizon = Color(0xFF34507A),
        onSky = Color(0xFFF4F1E4),
        isLight = false,
        showSun = false,
        sun = Color(0xFFF6F0D2),
        sunGlow = Color(0xFF8DA6D6),
        starAlpha = 0.9f,
        fireflies = true,
        cloudLight = Color(0xFF3B4E70),
        cloudShadow = Color(0xFF24324D),
        hills = listOf(Color(0xFF2D4260), Color(0xFF1E3047), Color(0xFF142235)),
    )
}
