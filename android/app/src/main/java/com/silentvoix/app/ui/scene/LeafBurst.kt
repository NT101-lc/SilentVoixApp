package com.silentvoix.app.ui.scene

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Where a leaf is at one moment, relative to the burst point, in units of the burst's size. */
data class LeafState(val x: Float, val y: Float, val rotation: Float, val alpha: Float)

/**
 * One leaf of a celebration: thrown up and out at [angle] (radians, screen coordinates, so upwards
 * is negative), then pulled down while it sways and spins, fading over the last part of the burst.
 */
class Leaf(
    private val angle: Float,
    private val speed: Float,
    private val spin: Float,
    private val sway: Float,
    val size: Float,
    val tint: Int,
) {
    fun at(progress: Float): LeafState {
        val p = progress.coerceIn(0f, 1f)
        val x = cos(angle) * speed * p + sway * sin(p * PI.toFloat() * 3f) * p
        val y = sin(angle) * speed * p + GRAVITY * p * p
        val alpha = if (p < FADE_FROM) 1f else 1f - (p - FADE_FROM) / (1f - FADE_FROM)
        return LeafState(x, y, rotation = spin * p, alpha = alpha.coerceIn(0f, 1f))
    }

    private companion object {
        const val GRAVITY = 1.4f
        const val FADE_FROM = 0.6f
    }
}

/** [count] leaves fanned upwards between 150° and 30° above the horizontal. */
fun leafBurst(seed: Int, count: Int): List<Leaf> {
    val random = Random(seed)
    return List(count) {
        val degrees = -150f + random.nextFloat() * 120f
        Leaf(
            angle = degrees * PI.toFloat() / 180f,
            speed = 0.6f + random.nextFloat() * 0.5f,
            spin = (random.nextFloat() - 0.5f) * 720f,
            sway = (random.nextFloat() - 0.5f) * 0.25f,
            size = 0.7f + random.nextFloat() * 0.6f,
            tint = random.nextInt(4),
        )
    }
}
