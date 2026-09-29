package com.silentvoix.app.speech

/** Speech rate bounds shared by the settings slider and the engine. 1.0 is the engine's normal speed. */
object SpeechRate {
    const val MIN = 0.5f
    const val MAX = 2.0f
    const val DEFAULT = 1.0f
}

enum class SpeechUnavailableReason { NO_ENGINE, LANGUAGE_MISSING }

sealed interface SpeechStatus {
    data object Initializing : SpeechStatus
    data object Ready : SpeechStatus
    data class Unavailable(val reason: SpeechUnavailableReason) : SpeechStatus
}

sealed interface SpeakOutcome {
    data object Spoken : SpeakOutcome

    /** The engine is still starting; the text is spoken once it is ready. */
    data object Queued : SpeakOutcome
    data object Failed : SpeakOutcome
    data class Unavailable(val reason: SpeechUnavailableReason) : SpeakOutcome
}

/** The text-to-speech engine, behind the few calls [SpeechController] needs. */
interface SpeechEngine {
    fun setRate(rate: Float)

    /** Speaks [text], interrupting anything already playing. Returns false on engine error. */
    fun speak(text: String): Boolean
    fun shutdown()
}

/**
 * Speech playback policy on top of a [SpeechEngine]: holds the latest request while the engine
 * starts, keeps the rate within [SpeechRate] bounds, and reports why speech is unavailable.
 * Call from the main thread.
 */
class SpeechController(private val engine: SpeechEngine) {
    var status: SpeechStatus = SpeechStatus.Initializing
        private set

    private var rate = SpeechRate.DEFAULT
    private var pending: String? = null

    /**
     * Called once the engine has started (or failed to). Returns the outcome for text queued
     * while it was starting, or null if nothing was queued.
     */
    fun onEngineInitialized(result: SpeechStatus): SpeakOutcome? {
        status = result
        if (result == SpeechStatus.Ready) engine.setRate(rate)
        val queued = pending ?: return null
        pending = null
        return speak(queued)
    }

    fun setRate(rate: Float) {
        this.rate = rate.coerceIn(SpeechRate.MIN, SpeechRate.MAX)
        if (status == SpeechStatus.Ready) engine.setRate(this.rate)
    }

    fun speak(text: String): SpeakOutcome = when (val current = status) {
        SpeechStatus.Initializing -> {
            pending = text
            SpeakOutcome.Queued
        }
        SpeechStatus.Ready -> if (engine.speak(text)) SpeakOutcome.Spoken else SpeakOutcome.Failed
        is SpeechStatus.Unavailable -> SpeakOutcome.Unavailable(current.reason)
    }

    fun release() {
        pending = null
        engine.shutdown()
    }
}
