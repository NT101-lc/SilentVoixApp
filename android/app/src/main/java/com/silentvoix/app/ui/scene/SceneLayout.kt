package com.silentvoix.app.ui.scene

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

// Everything a scene places, generated from a seed: the same seed paints the same picture on every
// device and in every screenshot. Positions are fractions of the scene's width and height.

/** One round puff of a cloud, relative to the cloud's centre, in cloud units. */
data class Puff(val dx: Float, val dy: Float, val radius: Float)

/**
 * A cumulus cloud: a cluster of [puffs] with a flat-ish base. [x] is where it starts across its
 * travel (0–1), [y] its height in the sky, [speed] how many scene widths it drifts per minute.
 */
data class Cloud(val x: Float, val y: Float, val scale: Float, val speed: Float, val puffs: List<Puff>) {

    /**
     * Size of one cloud unit, in pixels, for a scene [width] × [height]. Capped by the height, so a
     * wide, short scene (a tablet) does not get clouds taller than its sky.
     */
    fun unit(width: Float, height: Float): Float = min(width, height * 1.3f) * CLOUD_UNIT * scale

    /** Half the cloud's drawn width, in pixels. */
    fun halfWidth(width: Float, height: Float): Float = unit(width, height) * puffs.maxOf { abs(it.dx) + it.radius }
}

private const val CLOUD_UNIT = 0.04f

fun cloudLayout(seed: Int, count: Int): List<Cloud> {
    val random = Random(seed)
    return List(count) { index ->
        val puffCount = 4 + random.nextInt(3)
        val puffs = List(puffCount) { i ->
            // Puffs spread left to right, biggest in the middle, all resting on a common base.
            val across = (i - (puffCount - 1) / 2f) / ((puffCount - 1) / 2f)
            val radius = 0.55f + 0.45f * (1f - abs(across)) + random.nextFloat() * 0.15f
            Puff(dx = across * 1.25f, dy = -radius * 0.55f, radius = radius)
        }
        Cloud(
            // Spread evenly across the travel with a little jitter, so clouds never bunch up.
            x = ((index + random.nextFloat() * 0.6f) / count) % 1f,
            // Below the greeting across the top of the sky, above the far hills.
            y = 0.37f + random.nextFloat() * 0.13f,
            scale = 0.6f + random.nextFloat() * 0.8f,
            speed = 0.25f + random.nextFloat() * 0.45f,
            puffs = puffs,
        )
    }
}

/** The cloud's centre x in pixels at [timeMillis]; it drifts right and wraps in from the left. */
fun cloudX(cloud: Cloud, timeMillis: Long, width: Float, height: Float): Float {
    val half = cloud.halfWidth(width, height)
    val travel = width + 2 * half
    val moved = cloud.speed * width * (timeMillis / 60_000f)
    val position = (cloud.x * travel + moved) % travel
    return position - half
}

/**
 * Where the sun (or moon) sits, as fractions of the scene: low in the east at dawn and in the west
 * at dusk, and never in the top-right corner where the theme switch is.
 */
fun sunPosition(time: TimeOfDay): Pair<Float, Float> = when (time) {
    TimeOfDay.DAWN -> 0.2f to 0.46f
    TimeOfDay.DAY -> 0.66f to 0.33f
    TimeOfDay.DUSK -> 0.8f to 0.5f
    TimeOfDay.NIGHT -> 0.7f to 0.32f
}

data class Star(val x: Float, val y: Float, val radius: Float, val phase: Float)

fun stars(seed: Int, count: Int): List<Star> {
    val random = Random(seed)
    return List(count) {
        Star(
            x = random.nextFloat(),
            y = random.nextFloat() * 0.6f,
            radius = 0.6f + random.nextFloat() * 1.2f,
            phase = random.nextFloat() * 2f * PI.toFloat(),
        )
    }
}

/** A firefly hovering over the meadow; it drifts a little around ([x], [y]) and pulses. */
data class Firefly(val x: Float, val y: Float, val phase: Float, val periodMillis: Int, val drift: Float)

fun fireflies(seed: Int, count: Int): List<Firefly> {
    val random = Random(seed)
    return List(count) {
        Firefly(
            x = random.nextFloat(),
            y = 0.55f + random.nextFloat() * 0.4f,
            phase = random.nextFloat() * 2f * PI.toFloat(),
            periodMillis = 1_800 + random.nextInt(1_800),
            drift = 0.01f + random.nextFloat() * 0.02f,
        )
    }
}

/** How brightly [firefly] glows at [timeMillis], from 0 (dark) to 1. */
fun fireflyGlow(firefly: Firefly, timeMillis: Long): Float {
    val angle = 2f * PI.toFloat() * (timeMillis % firefly.periodMillis) / firefly.periodMillis + firefly.phase
    val wave = (sin(angle) + 1f) / 2f
    // Sharpened so a firefly spends most of its time dim and flashes briefly.
    return (wave * wave).coerceIn(0f, 1f)
}

/**
 * The top edge of one hill layer, sampled at [samples] points across the scene, as fractions of the
 * scene's height from the top. Layer 0 is farthest (highest on screen), layer 2 nearest (lowest).
 */
fun hillRidge(seed: Int, layer: Int, samples: Int): FloatArray {
    val random = Random(seed * 31 + layer)
    val base = 0.58f + layer * 0.11f
    val amplitude = 0.07f - layer * 0.015f
    val frequency = 1.1f + random.nextFloat() * 0.8f
    val phase = random.nextFloat() * 2f * PI.toFloat()
    val ripplePhase = random.nextFloat() * 2f * PI.toFloat()
    return FloatArray(samples) { i ->
        val t = i / (samples - 1f)
        val rolling = sin(2f * PI.toFloat() * frequency * t + phase)
        val ripple = sin(2f * PI.toFloat() * frequency * 2.7f * t + ripplePhase) * 0.3f
        (base + amplitude * (rolling + ripple) / 1.3f).coerceIn(0f, 1f)
    }
}
