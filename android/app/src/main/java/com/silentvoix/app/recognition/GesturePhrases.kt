package com.silentvoix.app.recognition

import androidx.annotation.StringRes
import com.silentvoix.app.R

/**
 * Phrases for the stock MediaPipe gesture model's categories. These are common hand gestures,
 * not Vietnamese Sign Language; a custom VSL model would replace this table.
 * The model's "None" category, and anything unknown, has no phrase.
 */
@StringRes
fun phraseFor(gestureLabel: String): Int? = when (gestureLabel) {
    "Open_Palm" -> R.string.gesture_open_palm
    "Thumb_Up" -> R.string.gesture_thumb_up
    "Thumb_Down" -> R.string.gesture_thumb_down
    "Victory" -> R.string.gesture_victory
    "Pointing_Up" -> R.string.gesture_pointing_up
    "Closed_Fist" -> R.string.gesture_closed_fist
    "ILoveYou" -> R.string.gesture_i_love_you
    else -> null
}
