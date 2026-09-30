package com.silentvoix.app.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.silentvoix.app.recognition.HandPose

/**
 * A hand's skeleton: bones as rounded strokes, joints as dots, fingertips a little larger.
 * [points] are x, y pixel pairs in landmark order. [outline], when given, is drawn as a soft edge
 * under the bones so the skeleton stays readable over a camera image.
 */
fun DrawScope.drawHand(
    points: FloatArray,
    bone: Color,
    joint: Color,
    alpha: Float = 1f,
    outline: Color? = null,
    strokeWidth: Float = 3.dp.toPx(),
) {
    fun at(index: Int) = Offset(points[index * 2], points[index * 2 + 1])
    if (outline != null) {
        HandPose.Bones.forEach { (from, to) ->
            drawLine(outline, at(from), at(to), strokeWidth = strokeWidth * 2f, cap = StrokeCap.Round, alpha = 0.45f)
        }
    }
    HandPose.Bones.forEach { (from, to) ->
        drawLine(bone, at(from), at(to), strokeWidth = strokeWidth, cap = StrokeCap.Round, alpha = alpha)
    }
    for (index in 0 until HandPose.LANDMARK_COUNT) {
        val tip = index in HandPose.Fingertips
        drawCircle(bone, radius = strokeWidth * (if (tip) 2.15f else 1.65f), center = at(index), alpha = alpha)
        drawCircle(joint, radius = strokeWidth * (if (tip) 1.15f else 0.85f), center = at(index), alpha = alpha)
    }
}

// The extent of HandPose.OpenPalm, the largest of the hand shapes.
private const val OpenPalmWidth = 0.74f
private const val OpenPalmHeight = 0.81f

/** A hand shape as a small illustration, scaled to fit whatever size it is given. Decorative. */
@Composable
fun HandGlyph(
    pose: HandPose,
    bone: Color,
    joint: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 2.5.dp,
    alpha: Float = 1f,
) {
    Canvas(modifier = modifier) {
        val stroke = strokeWidth.toPx()
        val source = pose.points
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (i in source.indices step 2) {
            minX = minOf(minX, source[i])
            maxX = maxOf(maxX, source[i])
            minY = minOf(minY, source[i + 1])
            maxY = maxOf(maxY, source[i + 1])
        }
        // Every hand is drawn at the scale that fits an open palm, centred on its own shape, so a
        // fist is not blown up to the size of a spread hand. The margin leaves room for the dots.
        val margin = stroke * 2.4f
        val scale = minOf((size.width - 2 * margin) / OpenPalmWidth, (size.height - 2 * margin) / OpenPalmHeight)
        val left = (size.width - (maxX - minX) * scale) / 2f
        val top = (size.height - (maxY - minY) * scale) / 2f
        val points = FloatArray(source.size) { i ->
            if (i % 2 == 0) left + (source[i] - minX) * scale else top + (source[i] - minY) * scale
        }
        drawHand(points, bone = bone, joint = joint, alpha = alpha, strokeWidth = stroke)
    }
}
