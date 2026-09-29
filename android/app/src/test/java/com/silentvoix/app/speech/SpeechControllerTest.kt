package com.silentvoix.app.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechControllerTest {

    private class FakeEngine : SpeechEngine {
        val spoken = mutableListOf<String>()
        var rate: Float? = null
        var speakSucceeds = true
        var shutDown = false

        override fun setRate(rate: Float) { this.rate = rate }
        override fun speak(text: String): Boolean {
            if (speakSucceeds) spoken += text
            return speakSucceeds
        }
        override fun shutdown() { shutDown = true }
    }

    private val engine = FakeEngine()
    private val controller = SpeechController(engine)

    @Test
    fun `speaks immediately once the engine is ready`() {
        controller.onEngineInitialized(SpeechStatus.Ready)

        assertEquals(SpeakOutcome.Spoken, controller.speak("Xin chào"))
        assertEquals(listOf("Xin chào"), engine.spoken)
    }

    @Test
    fun `text requested before the engine is ready is spoken when it becomes ready`() {
        assertEquals(SpeakOutcome.Queued, controller.speak("Xin chào"))
        assertTrue(engine.spoken.isEmpty())

        assertEquals(SpeakOutcome.Spoken, controller.onEngineInitialized(SpeechStatus.Ready))
        assertEquals(listOf("Xin chào"), engine.spoken)
    }

    @Test
    fun `only the latest queued text is spoken`() {
        controller.speak("Xin chào")
        controller.speak("Đồng ý")
        controller.onEngineInitialized(SpeechStatus.Ready)

        assertEquals(listOf("Đồng ý"), engine.spoken)
    }

    @Test
    fun `nothing queued means no outcome when the engine becomes ready`() {
        assertNull(controller.onEngineInitialized(SpeechStatus.Ready))
    }

    @Test
    fun `speaking while unavailable reports why and does not touch the engine`() {
        controller.onEngineInitialized(SpeechStatus.Unavailable(SpeechUnavailableReason.LANGUAGE_MISSING))

        assertEquals(
            SpeakOutcome.Unavailable(SpeechUnavailableReason.LANGUAGE_MISSING),
            controller.speak("Xin chào"),
        )
        assertTrue(engine.spoken.isEmpty())
    }

    @Test
    fun `a failed init reports the queued text as unavailable`() {
        controller.speak("Xin chào")

        val outcome = controller.onEngineInitialized(SpeechStatus.Unavailable(SpeechUnavailableReason.NO_ENGINE))

        assertEquals(SpeakOutcome.Unavailable(SpeechUnavailableReason.NO_ENGINE), outcome)
        assertTrue(engine.spoken.isEmpty())
    }

    @Test
    fun `rate is clamped to the supported range`() {
        controller.onEngineInitialized(SpeechStatus.Ready)

        controller.setRate(5f)
        assertEquals(SpeechRate.MAX, engine.rate)
        controller.setRate(0.1f)
        assertEquals(SpeechRate.MIN, engine.rate)
    }

    @Test
    fun `a rate set before the engine is ready is applied when it becomes ready`() {
        controller.setRate(1.5f)
        assertNull(engine.rate)

        controller.onEngineInitialized(SpeechStatus.Ready)
        assertEquals(1.5f, engine.rate)
    }

    @Test
    fun `an engine error is reported as failed`() {
        controller.onEngineInitialized(SpeechStatus.Ready)
        engine.speakSucceeds = false

        assertEquals(SpeakOutcome.Failed, controller.speak("Xin chào"))
    }

    @Test
    fun `release shuts the engine down`() {
        controller.release()

        assertTrue(engine.shutDown)
    }
}
