package com.silentvoix.app.ui.scene

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneLayoutTest {

    @Test
    fun `the same seed always paints the same clouds`() {
        assertEquals(cloudLayout(seed = 7, count = 5), cloudLayout(seed = 7, count = 5))
        assertNotEquals(cloudLayout(seed = 7, count = 5), cloudLayout(seed = 8, count = 5))
    }

    @Test
    fun `clouds float in the upper sky and are built from several puffs`() {
        val clouds = cloudLayout(seed = 3, count = 6)
        assertEquals(6, clouds.size)
        clouds.forEach { cloud ->
            assertTrue(cloud.y in 0.08f..0.5f)
            assertTrue(cloud.scale in 0.6f..1.4f)
            assertTrue(cloud.speed > 0f)
            assertTrue(cloud.puffs.size >= 4)
        }
    }

    @Test
    fun `clouds stay below the greeting written across the top of the sky`() {
        // The greeting takes roughly the top quarter of the scene; a cloud's top must stay under it.
        cloudLayout(seed = 7, count = 5).forEach { cloud ->
            val topOfCloud = cloud.y - cloud.puffs.maxOf { it.radius - it.dy } * cloud.scale * 0.04f * 1.3f
            assertTrue("cloud top at $topOfCloud", topOfCloud > 0.24f)
        }
    }

    @Test
    fun `the sun and moon keep clear of the top corner where the theme switch sits`() {
        TimeOfDay.entries.forEach { time ->
            val (x, y) = sunPosition(time)
            assertTrue("$time at ($x, $y)", !(x > 0.75f && y < 0.3f))
            assertTrue("$time stays above the far hills", y < 0.55f)
        }
    }

    @Test
    fun `a cloud drifts right and comes back in from the left`() {
        val cloud = cloudLayout(seed = 1, count = 1).single()
        val start = cloudX(cloud, timeMillis = 0, width = 1000f, height = 600f)
        val later = cloudX(cloud, timeMillis = 10_000, width = 1000f, height = 600f)
        assertTrue("drifts right", later > start)

        // Over a long time it always stays within the wrapped band around the screen.
        (0L..3_600_000L step 7_919L).forEach { t ->
            val x = cloudX(cloud, t, width = 1000f, height = 600f)
            val half = cloud.halfWidth(1000f, 600f)
            assertTrue("x=$x at $t", x >= -half * 1.01f && x <= 1000f + half * 1.01f)
        }
    }

    @Test
    fun `stars stay in the sky and fireflies over the meadow`() {
        stars(seed = 2, count = 40).forEach { assertTrue(it.y in 0f..0.6f) }
        fireflies(seed = 2, count = 12).forEach { assertTrue(it.y in 0.55f..0.95f) }
        assertEquals(stars(seed = 2, count = 40), stars(seed = 2, count = 40))
    }

    @Test
    fun `a firefly's glow pulses between dark and bright`() {
        val firefly = fireflies(seed = 4, count = 1).single()
        val glows = (0L..10_000L step 50L).map { fireflyGlow(firefly, it) }
        assertTrue(glows.all { it in 0f..1f })
        assertTrue("dims", glows.min() < 0.2f)
        assertTrue("brightens", glows.max() > 0.8f)
    }

    @Test
    fun `a hill's ridge stays within its band, nearer hills sitting lower`() {
        val far = hillRidge(seed = 5, layer = 0, samples = 64)
        val near = hillRidge(seed = 5, layer = 2, samples = 64)
        assertEquals(64, far.size)
        (far + near).forEach { assertTrue(it in 0f..1f) }
        assertTrue(near.average() > far.average())
    }
}
