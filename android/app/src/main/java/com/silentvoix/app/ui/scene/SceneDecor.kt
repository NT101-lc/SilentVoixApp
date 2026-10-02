package com.silentvoix.app.ui.scene

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.silentvoix.app.ui.common.rememberShouldAnimate
import com.silentvoix.app.ui.theme.StagePalette
import kotlin.random.Random

/**
 * A faint paper grain over a page: tiny specks scattered from a fixed seed, so the flat colour
 * reads like printed paper. Computed once per size; nothing animates.
 */
fun Modifier.paperGrain(ink: Color, alpha: Float): Modifier = drawWithCache {
    val random = Random(42)
    val count = (size.width * size.height / 1400f).toInt().coerceAtMost(6_000)
    val specks = List(count) { Offset(random.nextFloat() * size.width, random.nextFloat() * size.height) }
    onDrawBehind {
        drawPoints(specks, PointMode.Points, ink, strokeWidth = 1.4f * density, cap = StrokeCap.Round, alpha = alpha)
    }
}

/**
 * A wash of sky behind the top of a screen with a couple of soft clouds, fading into the page.
 * Light themes get a day sky and dark themes a night one, so a title written over it keeps the
 * page's own contrast.
 */
@Composable
fun Modifier.skyWash(height: Dp = 260.dp): Modifier {
    val background = MaterialTheme.colorScheme.background
    val night = background.luminance() < 0.2f
    val palette = skyPalette(if (night) TimeOfDay.NIGHT else TimeOfDay.DAY)
    val clouds = remember { cloudLayout(seed = 3, count = 3) }
    return drawBehind {
        val washHeight = height.toPx().coerceAtMost(size.height)
        drawRect(
            Brush.verticalGradient(
                0f to palette.skyMiddle.copy(alpha = if (night) 0.9f else 0.75f),
                1f to background.copy(alpha = 0f),
                endY = washHeight,
            ),
            size = Size(size.width, washHeight),
        )
        clouds.forEach { cloud ->
            val unit = cloud.unit(size.width, washHeight) * 0.8f
            val cx = cloudX(cloud, 0, size.width, washHeight)
            val cy = washHeight * (cloud.y - 0.25f)
            cloud.puffs.forEach { puff ->
                drawCircle(
                    palette.cloudLight,
                    radius = puff.radius * unit,
                    center = Offset(cx + puff.dx * unit, cy + puff.dy * unit),
                    alpha = if (night) 0.35f else 0.55f,
                )
            }
        }
    }
}

/**
 * The camera stage before it is switched on: a night sky with stars, a pool of moonlight where the
 * hand goes, and a few fireflies. [dimmed] quietens it behind an error or permission message.
 */
@Composable
fun NightStage(dimmed: Boolean, modifier: Modifier = Modifier, animate: Boolean = rememberShouldAnimate()) {
    val starField = remember { stars(seed = 21, count = 45) }
    val flies = remember { fireflies(seed = 21, count = 9) }
    val clock = rememberSceneClock(animate)
    Canvas(modifier = modifier) {
        val t = clock.longValue
        drawRect(Brush.verticalGradient(listOf(StagePalette.Ink, StagePalette.Glow.copy(alpha = 0.55f), StagePalette.Ink)))
        drawCircle(
            Brush.radialGradient(
                listOf(StagePalette.Glow, Color.Transparent),
                center = Offset(size.width / 2f, size.height * 0.4f),
                radius = size.maxDimension * 0.55f,
            ),
            radius = size.maxDimension * 0.55f,
            center = Offset(size.width / 2f, size.height * 0.4f),
            alpha = if (dimmed) 0.4f else 0.9f,
        )
        drawStars(starField, alpha = if (dimmed) 0.35f else 0.8f, t = t)
        if (!dimmed) drawFireflies(flies, t, strength = 0.8f)
    }
}

/**
 * A handful of leaves thrown up from the centre of this (zero-size is fine) box each time
 * [trigger] changes to a new positive value, falling and fading over [durationMillis]. Not
 * clipped, so place it where the leaves may fly over the surroundings.
 */
@Composable
fun LeafBurstEffect(
    trigger: Int,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    spread: Dp = 90.dp,
    durationMillis: Int = 1_400,
) {
    if (trigger <= 0) return
    val leaves = remember(trigger) { leafBurst(seed = trigger * 7919, count = 18) }
    val progress = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) { progress.animateTo(1f, tween(durationMillis, easing = LinearEasing)) }
    Canvas(modifier = modifier) {
        val p = progress.value
        if (p >= 1f) return@Canvas
        val scale = spread.toPx()
        leaves.forEach { leaf ->
            val state = leaf.at(p)
            val centre = center + Offset(state.x * scale, state.y * scale)
            val length = 9f * density * leaf.size
            rotate(state.rotation, pivot = centre) {
                drawOval(
                    colors[leaf.tint % colors.size],
                    topLeft = centre - Offset(length, length * 0.42f),
                    size = Size(length * 2f, length * 0.84f),
                    alpha = state.alpha,
                )
                drawLine(
                    Color.Black.copy(alpha = 0.18f * state.alpha),
                    start = centre - Offset(length * 0.8f, 0f),
                    end = centre + Offset(length * 0.8f, 0f),
                    strokeWidth = 0.8f * density,
                )
            }
        }
    }
}
