package com.silentvoix.app.recognition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GestureStabilizerTest {

    private val stabilizer = GestureStabilizer(requiredFrames = 3, minScore = 0.6f)

    private fun feed(vararg frames: FramePrediction?): List<StableGesture?> =
        frames.map { stabilizer.onFrame(it) }

    private fun frame(label: String, score: Float = 0.9f) = FramePrediction(label, score)

    @Test
    fun `emits a gesture only after it is held for the required frames`() {
        val out = feed(frame("Open_Palm"), frame("Open_Palm"), frame("Open_Palm"))

        assertNull(out[0])
        assertNull(out[1])
        assertEquals("Open_Palm", out[2]?.label)
    }

    @Test
    fun `reported confidence is the average score over the held frames`() {
        val out = feed(frame("Thumb_Up", 0.7f), frame("Thumb_Up", 0.8f), frame("Thumb_Up", 0.9f))

        assertEquals(0.8f, out[2]!!.confidence, 0.0001f)
    }

    @Test
    fun `a flickering label never reaches the threshold`() {
        val out = feed(
            frame("Open_Palm"), frame("Victory"), frame("Open_Palm"),
            frame("Victory"), frame("Open_Palm"), frame("Victory"),
        )

        assertEquals(List(6) { null }, out)
    }

    @Test
    fun `low confidence frames break the streak`() {
        val out = feed(frame("Open_Palm"), frame("Open_Palm", 0.3f), frame("Open_Palm"), frame("Open_Palm"))

        assertEquals(listOf(null, null, null, null), out)
    }

    @Test
    fun `a held gesture is emitted once, not on every frame`() {
        val out = feed(*Array(8) { frame("Closed_Fist") })

        assertEquals(1, out.count { it != null })
    }

    @Test
    fun `the same gesture is emitted again after the hand leaves the frame`() {
        feed(frame("Open_Palm"), frame("Open_Palm"), frame("Open_Palm"))
        val out = feed(null, frame("Open_Palm"), frame("Open_Palm"), frame("Open_Palm"))

        assertEquals("Open_Palm", out[3]?.label)
    }

    @Test
    fun `switching directly to another gesture emits the new one`() {
        feed(frame("Open_Palm"), frame("Open_Palm"), frame("Open_Palm"))
        val out = feed(frame("Thumb_Up"), frame("Thumb_Up"), frame("Thumb_Up"))

        assertEquals("Thumb_Up", out[2]?.label)
    }

    @Test
    fun `reset forgets the last emitted gesture`() {
        feed(frame("Open_Palm"), frame("Open_Palm"), frame("Open_Palm"))
        stabilizer.reset()
        val out = feed(frame("Open_Palm"), frame("Open_Palm"), frame("Open_Palm"))

        assertEquals("Open_Palm", out[2]?.label)
    }
}
