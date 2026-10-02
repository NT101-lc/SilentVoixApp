package com.silentvoix.app.ui.scene

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import com.silentvoix.app.ui.common.rememberShouldAnimate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Firefly light, shared with the camera stage's night sky. */
internal val FireflyColor = Color(0xFFE4F59E)

private const val FRAME_STEP_MILLIS = 32L

/**
 * Milliseconds since the scene appeared, ticking at about 30 fps while [animate] is true and
 * frozen at 0 otherwise. Read it only while drawing, so a tick redraws a canvas and nothing else.
 */
@Composable
internal fun rememberSceneClock(animate: Boolean): MutableLongState {
    val clock = remember { mutableLongStateOf(0L) }
    if (animate) {
        LaunchedEffect(Unit) {
            val start = withFrameMillis { it }
            var last = 0L
            while (true) {
                withFrameMillis { now ->
                    val elapsed = now - start
                    if (elapsed - last >= FRAME_STEP_MILLIS) {
                        last = elapsed
                        clock.longValue = elapsed
                    }
                }
            }
        }
    }
    return clock
}

/**
 * A painted countryside for [time]: a watercolour sky, the sun (or the moon and stars), cumulus
 * clouds drifting across, three layers of hills and a fringe of grass swaying at the front, with
 * fireflies after dark. [parallax] is how far the page has scrolled, in pixels: farther layers
 * move less than nearer ones, which gives the picture depth. Decorative only.
 */
@Composable
fun SkyScene(
    time: TimeOfDay,
    modifier: Modifier = Modifier,
    seed: Int = 7,
    parallax: () -> Float = { 0f },
    /** The page colour the meadow fades into at the bottom, or null for a hard edge. */
    fadeInto: Color? = null,
    animate: Boolean = rememberShouldAnimate(),
) {
    val palette = remember(time) { skyPalette(time) }
    val clouds = remember(seed) { cloudLayout(seed, count = 5) }
    val starField = remember(seed) { stars(seed, count = 60) }
    val flies = remember(seed) { fireflies(seed, count = 14) }
    val ridges = remember(seed) { List(3) { layer -> hillRidge(seed, layer, samples = 48) } }
    val clock = rememberSceneClock(animate)

    Canvas(modifier = modifier) {
        val t = clock.longValue
        val scroll = parallax()
        drawSky(palette)
        drawStars(starField, palette.starAlpha, t)
        drawSunOrMoon(time, palette, offsetY = scroll * 0.6f)
        drawClouds(clouds, palette, t, offsetY = scroll * 0.45f)
        ridges.forEachIndexed { layer, ridge ->
            // Far hills drift with the page more slowly than near ones.
            drawHill(ridge, palette.hills[layer], offsetY = scroll * (0.3f - layer * 0.1f))
        }
        drawGrass(ridges[2], palette.hills[2], t)
        if (palette.fireflies) drawFireflies(flies, t, strength = if (time == TimeOfDay.NIGHT) 1f else 0.6f)
        if (fadeInto != null) {
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, fadeInto), startY = size.height * 0.8f, endY = size.height))
        }
    }
}

private fun DrawScope.drawSky(palette: SkyPalette) {
    drawRect(
        Brush.verticalGradient(
            0f to palette.skyTop,
            0.45f to palette.skyMiddle,
            0.78f to palette.skyHorizon,
        ),
    )
}

internal fun DrawScope.drawStars(stars: List<Star>, alpha: Float, t: Long, color: Color = Color(0xFFFFF8E1)) {
    if (alpha <= 0f) return
    stars.forEach { star ->
        val twinkle = 0.55f + 0.45f * sin(t / 900f + star.phase)
        drawCircle(
            color = color,
            radius = star.radius * density,
            center = Offset(star.x * size.width, star.y * size.height),
            alpha = (alpha * twinkle).coerceIn(0f, 1f),
        )
    }
}

private fun DrawScope.drawSunOrMoon(time: TimeOfDay, palette: SkyPalette, offsetY: Float) {
    val (fx, fy) = sunPosition(time)
    val radius = min(size.width, size.height) * 0.07f
    val centre = Offset(size.width * fx, size.height * fy + offsetY)
    drawCircle(
        Brush.radialGradient(listOf(palette.sunGlow.copy(alpha = 0.55f), Color.Transparent), centre, radius * 4.2f),
        radius = radius * 4.2f,
        center = centre,
    )
    drawCircle(palette.sun, radius = radius, center = centre)
    if (!palette.showSun) {
        // The moon's soft grey seas.
        val sea = palette.sunGlow.copy(alpha = 0.22f)
        drawCircle(sea, radius = radius * 0.28f, center = centre + Offset(-radius * 0.3f, -radius * 0.2f))
        drawCircle(sea, radius = radius * 0.18f, center = centre + Offset(radius * 0.35f, radius * 0.25f))
        drawCircle(sea, radius = radius * 0.12f, center = centre + Offset(-radius * 0.1f, radius * 0.45f))
    }
}

private fun DrawScope.drawClouds(clouds: List<Cloud>, palette: SkyPalette, t: Long, offsetY: Float) {
    clouds.forEach { cloud ->
        val unit = cloud.unit(size.width, size.height)
        val cx = cloudX(cloud, t, size.width, size.height)
        val cy = cloud.y * size.height + offsetY
        val half = cloud.halfWidth(size.width, size.height)
        // Shadow side first, a little lower; the lit side over it; both cut flat at the base.
        clipRect(left = cx - half - unit, top = cy - unit * 3f, right = cx + half + unit, bottom = cy + unit * 0.42f) {
            cloud.puffs.forEach { puff ->
                drawCircle(
                    palette.cloudShadow,
                    radius = puff.radius * unit,
                    center = Offset(cx + puff.dx * unit, cy + puff.dy * unit + unit * 0.16f),
                    alpha = 0.8f,
                )
            }
        }
        clipRect(left = cx - half - unit, top = cy - unit * 3f, right = cx + half + unit, bottom = cy + unit * 0.22f) {
            cloud.puffs.forEach { puff ->
                drawCircle(
                    palette.cloudLight,
                    radius = puff.radius * unit * 0.94f,
                    center = Offset(cx + puff.dx * unit - unit * 0.06f, cy + puff.dy * unit),
                )
            }
        }
    }
}

/** A hill filled from its ridge down to the bottom, with the ridge smoothed into curves. */
private fun DrawScope.drawHill(ridge: FloatArray, color: Color, offsetY: Float) {
    val step = size.width / (ridge.size - 1)
    val path = Path().apply {
        moveTo(0f, size.height)
        lineTo(0f, ridge[0] * size.height + offsetY)
        for (i in 1 until ridge.size) {
            val x0 = (i - 1) * step
            val y0 = ridge[i - 1] * size.height + offsetY
            val x1 = i * step
            val y1 = ridge[i] * size.height + offsetY
            quadraticTo(x0, y0, (x0 + x1) / 2f, (y0 + y1) / 2f)
        }
        lineTo(size.width, ridge.last() * size.height + offsetY)
        lineTo(size.width, size.height)
        close()
    }
    drawPath(path, color)
    // A lighter rim where the light catches the top of the hill.
    drawPath(path, Brush.verticalGradient(listOf(lerp(color, Color.White, 0.18f), color), startY = ridge.min() * size.height + offsetY, endY = ridge.max() * size.height + offsetY + 40f), alpha = 0.6f)
}

/** Blades along the front hill's ridge, each leaning with a slow breeze. */
private fun DrawScope.drawGrass(ridge: FloatArray, hill: Color, t: Long) {
    val blade = lerp(hill, Color.Black, 0.1f)
    val spacing = 3.5f * density
    var x = 0f
    var index = 0
    while (x <= size.width) {
        val position = x / size.width * (ridge.size - 1)
        val i = position.toInt().coerceAtMost(ridge.size - 2)
        val y = (ridge[i] + (ridge[i + 1] - ridge[i]) * (position - i)) * size.height + 2f * density
        val height = (4f + (index * 37 % 5)) * density
        val lean = sin(t / 1300f + x * 0.012f) * 0.18f + ((index * 53 % 5) - 2) * 0.04f
        drawLine(
            blade,
            start = Offset(x, y),
            end = Offset(x + sin(lean) * height, y - cos(lean) * height),
            strokeWidth = 1.3f * density,
            cap = StrokeCap.Round,
        )
        x += spacing
        index++
    }
}

internal fun DrawScope.drawFireflies(flies: List<Firefly>, t: Long, strength: Float) {
    flies.forEach { fly ->
        val glow = fireflyGlow(fly, t) * strength
        if (glow < 0.02f) return@forEach
        val drift = 2f * PI.toFloat() * t / (fly.periodMillis * 3f)
        val centre = Offset(
            (fly.x + fly.drift * sin(drift + fly.phase)) * size.width,
            (fly.y + fly.drift * cos(drift * 0.7f + fly.phase)) * size.height,
        )
        drawCircle(
            Brush.radialGradient(listOf(FireflyColor.copy(alpha = 0.55f * glow), Color.Transparent), centre, 9f * density),
            radius = 9f * density,
            center = centre,
        )
        drawCircle(FireflyColor, radius = 1.8f * density, center = centre, alpha = glow)
    }
}
