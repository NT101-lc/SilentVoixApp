package com.silentvoix.app.ui.common

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionTest {

    @Test
    fun `animations stop when the system turns them off`() {
        assertFalse(shouldAnimate(animatorDurationScale = 0f))
    }

    @Test
    fun `slowed or normal animations still play`() {
        assertTrue(shouldAnimate(animatorDurationScale = 0.5f))
        assertTrue(shouldAnimate(animatorDurationScale = 1f))
        assertTrue(shouldAnimate(animatorDurationScale = 10f))
    }
}
