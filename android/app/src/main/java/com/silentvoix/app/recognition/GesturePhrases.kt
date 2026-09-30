package com.silentvoix.app.recognition

import androidx.annotation.StringRes
import com.silentvoix.app.R
import com.silentvoix.app.recognition.HandPose.Companion.Thumb

/**
 * One gesture the stock MediaPipe model recognises: its category [label], what to call it
 * ([nameRes]), the phrase it stands for ([phraseRes]) and a drawing of the hand shape ([pose]).
 */
class SupportedGesture(
    val label: String,
    @StringRes val nameRes: Int,
    @StringRes val phraseRes: Int,
    val pose: HandPose,
)

/**
 * The stock model's categories. These are common hand gestures, not Vietnamese Sign Language; a
 * custom VSL model would replace this table. The model's "None" category is not listed.
 */
val SupportedGestures: List<SupportedGesture> = listOf(
    SupportedGesture(
        "Open_Palm", R.string.gesture_name_open_palm, R.string.gesture_open_palm,
        HandPose.glyph(Thumb.OUT, index = true, middle = true, ring = true, little = true),
    ),
    SupportedGesture(
        "Thumb_Up", R.string.gesture_name_thumb_up, R.string.gesture_thumb_up,
        HandPose.glyph(Thumb.UP, index = false, middle = false, ring = false, little = false),
    ),
    SupportedGesture(
        "Thumb_Down", R.string.gesture_name_thumb_down, R.string.gesture_thumb_down,
        HandPose.glyph(Thumb.UP, index = false, middle = false, ring = false, little = false).flippedVertically(),
    ),
    SupportedGesture(
        "Victory", R.string.gesture_name_victory, R.string.gesture_victory,
        HandPose.glyph(Thumb.FOLDED, index = true, middle = true, ring = false, little = false),
    ),
    SupportedGesture(
        "Pointing_Up", R.string.gesture_name_pointing_up, R.string.gesture_pointing_up,
        HandPose.glyph(Thumb.FOLDED, index = true, middle = false, ring = false, little = false),
    ),
    SupportedGesture(
        "Closed_Fist", R.string.gesture_name_closed_fist, R.string.gesture_closed_fist,
        HandPose.glyph(Thumb.FOLDED, index = false, middle = false, ring = false, little = false),
    ),
    SupportedGesture(
        "ILoveYou", R.string.gesture_name_i_love_you, R.string.gesture_i_love_you,
        HandPose.glyph(Thumb.OUT, index = true, middle = false, ring = false, little = true),
    ),
)

/** The phrase for a model category; "None", and anything unknown, has no phrase. */
@StringRes
fun phraseFor(gestureLabel: String): Int? = SupportedGestures.firstOrNull { it.label == gestureLabel }?.phraseRes
