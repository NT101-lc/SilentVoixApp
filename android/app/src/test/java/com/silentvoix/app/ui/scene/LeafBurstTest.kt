package com.silentvoix.app.ui.scene

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LeafBurstTest {

    private val leaves = leafBurst(seed = 11, count = 24)

    @Test
    fun `every leaf starts at the burst point, fully visible`() {
        leaves.forEach { leaf ->
            val start = leaf.at(0f)
            assertEquals(0f, start.x, 1e-4f)
            assertEquals(0f, start.y, 1e-4f)
            assertEquals(1f, start.alpha, 1e-4f)
        }
    }

    @Test
    fun `leaves fly outwards, then fall and fade by the end`() {
        leaves.forEach { leaf ->
            val early = leaf.at(0.2f)
            val end = leaf.at(1f)
            assertTrue("moves away", early.x * early.x + early.y * early.y > 0f)
            assertTrue("falls", end.y > early.y)
            assertEquals(0f, end.alpha, 1e-4f)
        }
    }

    @Test
    fun `leaves scatter in many directions`() {
        val early = leaves.map { it.at(0.2f) }
        assertTrue(early.any { it.x < 0f } && early.any { it.x > 0f })
    }

    @Test
    fun `progress outside zero to one is clamped`() {
        val leaf = leaves.first()
        assertEquals(leaf.at(1f), leaf.at(3f))
        assertEquals(leaf.at(0f), leaf.at(-1f))
    }
}
