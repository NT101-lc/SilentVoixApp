package com.silentvoix.app.recognition

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HandPoseTest {

    /** Every landmark at the same spot, so a test can follow one point through the maths. */
    private fun allAt(x: Float, y: Float) =
        FloatArray(HandPose.LANDMARK_COUNT * 2) { if (it % 2 == 0) x else y }

    private fun HandPose.first() = points[0] to points[1]

    // A 640x480 sensor image with a point near its left edge, a quarter of the way down.
    private val sensorPoint = allAt(0.1f, 0.25f)

    @Test
    fun `an upright sensor image keeps its coordinates and aspect`() {
        val pose = HandPose.fromSensor(sensorPoint, rotationDegrees = 0, sensorWidth = 640, sensorHeight = 480)
        assertEquals(0.1f to 0.25f, pose.first())
        assertEquals(640f / 480f, pose.imageAspect, 1e-6f)
    }

    // Expected values match what MediaPipe itself returns for the same photo fed in rotated:
    // the model answers in the sensor image's frame, so the upright position is derived here.
    @Test
    fun `a quarter turn clockwise moves the left edge to the top`() {
        val pose = HandPose.fromSensor(sensorPoint, rotationDegrees = 90, sensorWidth = 640, sensorHeight = 480)
        assertEquals(0.75f to 0.1f, pose.first())
        assertEquals(480f / 640f, pose.imageAspect, 1e-6f)
    }

    @Test
    fun `a half turn flips both axes`() {
        val pose = HandPose.fromSensor(sensorPoint, rotationDegrees = 180, sensorWidth = 640, sensorHeight = 480)
        assertEquals(0.9f to 0.75f, pose.first())
        assertEquals(640f / 480f, pose.imageAspect, 1e-6f)
    }

    @Test
    fun `three quarter turns move the left edge to the bottom`() {
        val pose = HandPose.fromSensor(sensorPoint, rotationDegrees = 270, sensorWidth = 640, sensorHeight = 480)
        assertEquals(0.25f to 0.9f, pose.first())
        assertEquals(480f / 640f, pose.imageAspect, 1e-6f)
    }

    @Test
    fun `a view with the image's shape maps landmarks straight to pixels`() {
        val view = HandPose(allAt(0.25f, 0.5f), imageAspect = 3f / 4f).toViewPoints(300f, 400f)
        assertArrayEquals(floatArrayOf(75f, 200f), view.copyOfRange(0, 2), 1e-3f)
    }

    @Test
    fun `a taller view crops the image's sides`() {
        // A 3:4 image filling a 300x800 view is drawn 600 wide, so 150 px hang off each side.
        val view = HandPose(allAt(0.25f, 0.5f), imageAspect = 3f / 4f).toViewPoints(300f, 800f)
        assertArrayEquals(floatArrayOf(0f, 400f), view.copyOfRange(0, 2), 1e-3f)
    }

    @Test
    fun `a wider view crops the image's top and bottom`() {
        // A 3:4 image filling a 600x400 view is drawn 800 tall, so 200 px hang off top and bottom.
        val view = HandPose(allAt(0.25f, 0.5f), imageAspect = 3f / 4f).toViewPoints(600f, 400f)
        assertArrayEquals(floatArrayOf(150f, 200f), view.copyOfRange(0, 2), 1e-3f)
    }

    @Test
    fun `a mirrored preview flips landmarks left to right`() {
        val pose = HandPose(allAt(0.25f, 0.5f), imageAspect = 3f / 4f).withMirror(true)
        assertArrayEquals(floatArrayOf(225f, 200f), pose.toViewPoints(300f, 400f).copyOfRange(0, 2), 1e-3f)
    }

    private fun HandPose.y(landmark: Int) = points[landmark * 2 + 1]

    @Test
    fun `a glyph's straight finger reaches well above its knuckle and a folded one stays near it`() {
        val pointing = HandPose.glyph(HandPose.Companion.Thumb.FOLDED, index = true, middle = false, ring = false, little = false)
        val indexKnuckle = 5
        val indexTip = 8
        val middleKnuckle = 9
        val middleTip = 12
        assertTrue(pointing.y(indexKnuckle) - pointing.y(indexTip) > 0.3f)
        assertTrue(pointing.y(middleKnuckle) - pointing.y(middleTip) < 0.15f)
    }

    @Test
    fun `flipping a hand turns it upside down and back again`() {
        val up = HandPose.glyph(HandPose.Companion.Thumb.UP, index = false, middle = false, ring = false, little = false)
        val down = up.flippedVertically()
        val thumbTip = 4
        val wrist = 0
        assertTrue(up.y(thumbTip) < up.y(wrist))
        assertTrue(down.y(thumbTip) > down.y(wrist))
        assertArrayEquals(up.points, down.flippedVertically().points, 1e-6f)
    }

    @Test
    fun `every supported gesture has a complete hand shape and a distinct label`() {
        assertEquals(7, SupportedGestures.size)
        assertEquals(SupportedGestures.size, SupportedGestures.map { it.label }.toSet().size)
        assertEquals(SupportedGestures.size, SupportedGestures.map { it.pose.points.toList() }.toSet().size)
    }
}
