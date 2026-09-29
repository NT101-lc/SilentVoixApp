package com.silentvoix.app.speech

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/** Compose-facing speech: observable [status] plus the [SpeechController] calls. Main thread only. */
@Stable
class Speech internal constructor(
    context: Context,
    private val onDeferredOutcome: (SpeakOutcome) -> Unit,
) {
    var status: SpeechStatus by mutableStateOf(SpeechStatus.Initializing)
        private set

    private val controller: SpeechController = SpeechController(
        AndroidSpeechEngine(context) { result ->
            val outcome = controller.onEngineInitialized(result)
            status = controller.status
            outcome?.let(onDeferredOutcome)
        },
    )

    fun speak(text: String): SpeakOutcome = controller.speak(text)

    fun setRate(rate: Float) = controller.setRate(rate)

    internal fun release() = controller.release()
}

/**
 * App-scoped speech, released when it leaves the composition. [onDeferredOutcome] reports the
 * result of text that was queued while the engine was still starting.
 */
@Composable
fun rememberSpeech(onDeferredOutcome: (SpeakOutcome) -> Unit): Speech {
    val context = LocalContext.current.applicationContext
    val currentOnDeferredOutcome by rememberUpdatedState(onDeferredOutcome)
    val speech = remember { Speech(context) { currentOnDeferredOutcome(it) } }
    DisposableEffect(speech) {
        onDispose { speech.release() }
    }
    return speech
}
