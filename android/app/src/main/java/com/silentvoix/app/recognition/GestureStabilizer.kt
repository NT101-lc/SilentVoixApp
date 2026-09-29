package com.silentvoix.app.recognition

/** The model's top gesture for one camera frame. */
data class FramePrediction(val label: String, val score: Float)

/** A gesture held steadily enough to show to the user. [confidence] is 0..1. */
data class StableGesture(val label: String, val confidence: Float)

/**
 * Turns noisy per-frame predictions into one result per deliberate gesture: a label must win
 * [requiredFrames] frames in a row, each scoring at least [minScore], and is then emitted once
 * until the hand leaves the frame or changes gesture.
 *
 * Not thread-safe; call it from the single thread that delivers model results.
 */
class GestureStabilizer(
    private val requiredFrames: Int = 5,
    private val minScore: Float = 0.6f,
) {
    private var streakLabel: String? = null
    private val streakScores = ArrayDeque<Float>()
    private var lastEmitted: String? = null

    /** Pass `null` when no hand (or no known gesture) is in the frame. */
    fun onFrame(prediction: FramePrediction?): StableGesture? {
        if (prediction == null || prediction.score < minScore) {
            clearStreak()
            if (prediction == null) lastEmitted = null
            return null
        }

        if (prediction.label != streakLabel) {
            clearStreak()
            streakLabel = prediction.label
        }
        streakScores += prediction.score
        if (streakScores.size > requiredFrames) streakScores.removeFirst()

        if (streakScores.size < requiredFrames || prediction.label == lastEmitted) return null

        lastEmitted = prediction.label
        return StableGesture(prediction.label, streakScores.average().toFloat())
    }

    fun reset() {
        clearStreak()
        lastEmitted = null
    }

    private fun clearStreak() {
        streakLabel = null
        streakScores.clear()
    }
}
