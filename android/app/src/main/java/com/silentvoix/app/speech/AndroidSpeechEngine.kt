package com.silentvoix.app.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

private val Vietnamese: Locale = Locale.forLanguageTag("vi-VN")

/**
 * [SpeechEngine] backed by the device's [TextToSpeech] service, speaking Vietnamese.
 * [onInitialized] runs on the main thread once the service is bound and the voice checked.
 */
class AndroidSpeechEngine(
    context: Context,
    onInitialized: (SpeechStatus) -> Unit,
) : SpeechEngine {

    private var utteranceCount = 0
    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(context.applicationContext) { initStatus ->
            onInitialized(
                when {
                    initStatus != TextToSpeech.SUCCESS ->
                        SpeechStatus.Unavailable(SpeechUnavailableReason.NO_ENGINE)
                    tts.setLanguage(Vietnamese) < TextToSpeech.LANG_AVAILABLE ->
                        SpeechStatus.Unavailable(SpeechUnavailableReason.LANGUAGE_MISSING)
                    else -> SpeechStatus.Ready
                },
            )
        }
    }

    override fun setRate(rate: Float) {
        tts.setSpeechRate(rate)
    }

    override fun speak(text: String): Boolean =
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "silentvoix-${++utteranceCount}") ==
            TextToSpeech.SUCCESS

    override fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
