package com.silentvoix.app.ui.scene

import androidx.compose.ui.graphics.luminance
import com.silentvoix.app.ui.theme.contrastRatio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SkyTest {

    @Test
    fun `each hour of the day falls in one part of the day`() {
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.from(0))
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.from(4))
        assertEquals(TimeOfDay.DAWN, TimeOfDay.from(5))
        assertEquals(TimeOfDay.DAWN, TimeOfDay.from(7))
        assertEquals(TimeOfDay.DAY, TimeOfDay.from(8))
        assertEquals(TimeOfDay.DAY, TimeOfDay.from(16))
        assertEquals(TimeOfDay.DUSK, TimeOfDay.from(17))
        assertEquals(TimeOfDay.DUSK, TimeOfDay.from(18))
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.from(19))
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.from(23))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `an hour outside the clock is a mistake`() {
        TimeOfDay.from(24)
    }

    @Test
    fun `the sun is out by day and the moon, stars and fireflies by night`() {
        with(skyPalette(TimeOfDay.DAY)) {
            assertTrue(showSun)
            assertEquals(0f, starAlpha)
            assertFalse(fireflies)
        }
        with(skyPalette(TimeOfDay.NIGHT)) {
            assertFalse(showSun)
            assertTrue(starAlpha > 0.5f)
            assertTrue(fireflies)
        }
    }

    @Test
    fun `the greeting over the sky is readable at every hour`() {
        TimeOfDay.entries.forEach { time ->
            with(skyPalette(time)) {
                // The greeting sits over the upper sky, so both ends of that gradient must carry it.
                assertTrue("$time top", contrastRatio(onSky, skyTop) >= 4.5f)
                assertTrue("$time middle", contrastRatio(onSky, skyMiddle) >= 4.5f)
            }
        }
    }

    @Test
    fun `the status bar icons follow how light the sky is`() {
        assertTrue(skyPalette(TimeOfDay.DAY).isLight)
        assertFalse(skyPalette(TimeOfDay.NIGHT).isLight)
        TimeOfDay.entries.forEach { time ->
            with(skyPalette(time)) { assertEquals("$time", skyTop.luminance() > 0.4f, isLight) }
        }
    }

    @Test
    fun `hills get darker towards the front, giving depth`() {
        TimeOfDay.entries.forEach { time ->
            val hills = skyPalette(time).hills
            assertEquals(3, hills.size)
            assertTrue("$time", hills[0].luminance() > hills[1].luminance())
            assertTrue("$time", hills[1].luminance() > hills[2].luminance())
        }
    }
}
