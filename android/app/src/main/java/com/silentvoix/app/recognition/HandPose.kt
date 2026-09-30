package com.silentvoix.app.recognition

/**
 * The 21 hand landmarks of one frame, for drawing the hand's skeleton over the camera preview.
 *
 * [points] holds x, y pairs in MediaPipe's landmark order (wrist, then thumb to little finger),
 * normalised to 0..1 across the upright camera image. [imageAspect] is that image's width / height.
 * [mirrored] is true when the preview shows the image flipped left-to-right (front camera).
 */
class HandPose(
    val points: FloatArray,
    val imageAspect: Float,
    val mirrored: Boolean = false,
) {
    init {
        require(points.size == LANDMARK_COUNT * 2) { "Expected $LANDMARK_COUNT landmarks" }
    }

    /** The same hand turned upside down (thumbs up becomes thumbs down). */
    fun flippedVertically(): HandPose =
        HandPose(FloatArray(points.size) { i -> if (i % 2 == 0) points[i] else 1f - points[i] }, imageAspect, mirrored)

    fun withMirror(mirrored: Boolean): HandPose =
        if (mirrored == this.mirrored) this else HandPose(points, imageAspect, mirrored)

    /**
     * Landmarks as x, y pixel pairs inside a [viewWidth] × [viewHeight] view that shows the camera
     * image scaled to fill and centre-cropped, which is how the preview is drawn.
     */
    fun toViewPoints(viewWidth: Float, viewHeight: Float): FloatArray {
        val fillsWidth = viewWidth / viewHeight > imageAspect
        val drawnWidth = if (fillsWidth) viewWidth else viewHeight * imageAspect
        val drawnHeight = if (fillsWidth) viewWidth / imageAspect else viewHeight
        val left = (viewWidth - drawnWidth) / 2f
        val top = (viewHeight - drawnHeight) / 2f
        return FloatArray(points.size) { i ->
            if (i % 2 == 0) {
                val x = if (mirrored) 1f - points[i] else points[i]
                left + x * drawnWidth
            } else {
                top + points[i] * drawnHeight
            }
        }
    }

    companion object {
        const val LANDMARK_COUNT = 21

        /**
         * Builds a pose from landmarks as the model reports them: normalised to the camera sensor's
         * image, before the [rotationDegrees] (clockwise, a multiple of 90) that make it upright.
         */
        fun fromSensor(
            sensorPoints: FloatArray,
            rotationDegrees: Int,
            sensorWidth: Int,
            sensorHeight: Int,
        ): HandPose {
            val turns = ((rotationDegrees % 360) + 360) % 360 / 90
            val upright = FloatArray(sensorPoints.size)
            for (i in sensorPoints.indices step 2) {
                val x = sensorPoints[i]
                val y = sensorPoints[i + 1]
                when (turns) {
                    1 -> { upright[i] = 1f - y; upright[i + 1] = x }
                    2 -> { upright[i] = 1f - x; upright[i + 1] = 1f - y }
                    3 -> { upright[i] = y; upright[i + 1] = 1f - x }
                    else -> { upright[i] = x; upright[i + 1] = y }
                }
            }
            val sideways = turns % 2 == 1
            val width = if (sideways) sensorHeight else sensorWidth
            val height = if (sideways) sensorWidth else sensorHeight
            return HandPose(upright, width.toFloat() / height)
        }

        /** Landmark index pairs joined by a bone: the palm outline and the four joints per finger. */
        val Bones: List<Pair<Int, Int>> = listOf(
            0 to 1, 1 to 2, 2 to 3, 3 to 4,
            0 to 5, 5 to 6, 6 to 7, 7 to 8,
            5 to 9, 9 to 10, 10 to 11, 11 to 12,
            9 to 13, 13 to 14, 14 to 15, 15 to 16,
            13 to 17, 0 to 17, 17 to 18, 18 to 19, 19 to 20,
        )

        val Fingertips: Set<Int> = setOf(4, 8, 12, 16, 20)

        /**
         * A stylised hand for illustrations: each finger is either straight or folded into the
         * palm. Not anatomically exact, and not sized to any frame: draw it fitted to its bounds.
         */
        fun glyph(thumb: Thumb, index: Boolean, middle: Boolean, ring: Boolean, little: Boolean): HandPose {
            val points = FloatArray(LANDMARK_COUNT * 2)
            fun put(landmark: Int, x: Float, y: Float) {
                points[landmark * 2] = x
                points[landmark * 2 + 1] = y
            }
            put(0, 0.50f, 0.93f)
            val thumbJoints = when (thumb) {
                Thumb.OUT -> floatArrayOf(0.35f, 0.85f, 0.24f, 0.74f, 0.17f, 0.62f, 0.11f, 0.52f)
                Thumb.UP -> floatArrayOf(0.35f, 0.85f, 0.25f, 0.72f, 0.22f, 0.54f, 0.22f, 0.36f)
                Thumb.FOLDED -> floatArrayOf(0.36f, 0.85f, 0.28f, 0.75f, 0.36f, 0.68f, 0.48f, 0.67f)
            }
            for (joint in 0 until 4) put(1 + joint, thumbJoints[joint * 2], thumbJoints[joint * 2 + 1])
            GlyphFingers.forEachIndexed { finger, shape ->
                val straight = when (finger) {
                    0 -> index
                    1 -> middle
                    2 -> ring
                    else -> little
                }
                val first = 5 + finger * 4
                var x = shape.knuckleX
                var y = shape.knuckleY
                put(first, x, y)
                shape.segments.forEachIndexed { segment, length ->
                    // A folded finger shows only its first bone; the rest curls out of sight behind it.
                    val along = when {
                        straight -> length
                        segment == 0 -> length * 0.62f
                        else -> 0f
                    }
                    x += shape.leanX * along
                    y -= along
                    put(first + segment + 1, x, y)
                }
            }
            return HandPose(points, imageAspect = 1f)
        }

        enum class Thumb { OUT, UP, FOLDED }

        /** A finger of [glyph]: where its knuckle sits, how far it leans sideways, segment lengths. */
        private class GlyphFinger(val knuckleX: Float, val knuckleY: Float, val leanX: Float, val segments: FloatArray)

        private val GlyphFingers = listOf(
            GlyphFinger(0.37f, 0.56f, -0.12f, floatArrayOf(0.16f, 0.11f, 0.10f)),
            GlyphFinger(0.50f, 0.53f, 0f, floatArrayOf(0.18f, 0.12f, 0.11f)),
            GlyphFinger(0.62f, 0.56f, 0.10f, floatArrayOf(0.16f, 0.11f, 0.095f)),
            GlyphFinger(0.72f, 0.63f, 0.24f, floatArrayOf(0.13f, 0.09f, 0.08f)),
        )

        /** An open palm, used as the placement hint before the camera starts. */
        val OpenPalm = HandPose(
            points = floatArrayOf(
                0.50f, 0.93f,
                0.35f, 0.85f, 0.24f, 0.74f, 0.17f, 0.62f, 0.11f, 0.52f,
                0.37f, 0.56f, 0.33f, 0.40f, 0.31f, 0.29f, 0.29f, 0.19f,
                0.50f, 0.53f, 0.50f, 0.35f, 0.50f, 0.23f, 0.50f, 0.12f,
                0.62f, 0.56f, 0.65f, 0.40f, 0.67f, 0.29f, 0.68f, 0.20f,
                0.72f, 0.63f, 0.78f, 0.51f, 0.82f, 0.42f, 0.85f, 0.34f,
            ),
            imageAspect = 1f,
        )
    }
}
