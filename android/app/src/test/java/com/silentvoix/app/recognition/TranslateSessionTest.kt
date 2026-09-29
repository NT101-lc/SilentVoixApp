package com.silentvoix.app.recognition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslateSessionTest {

    private val hello = Recognition("Xin chào", 92)
    private val listening = TranslateSession().onStartRequested(hasCameraPermission = true).onEngineReady()

    @Test
    fun `starting with permission starts the camera and engine`() {
        val session = TranslateSession().onStartRequested(hasCameraPermission = true)

        assertEquals(SessionStatus.Starting, session.status)
        assertTrue(session.isRunning)
    }

    @Test
    fun `starting without permission waits for the permission prompt`() {
        val session = TranslateSession().onStartRequested(hasCameraPermission = false)

        assertEquals(SessionStatus.AwaitingPermission, session.status)
        assertFalse(session.isRunning)
    }

    @Test
    fun `granting permission starts, denying it shows the denied state`() {
        val waiting = TranslateSession().onStartRequested(hasCameraPermission = false)

        assertEquals(SessionStatus.Starting, waiting.onPermissionResult(granted = true).status)
        val denied = waiting.onPermissionResult(granted = false)
        assertEquals(SessionStatus.PermissionDenied, denied.status)
        assertFalse(denied.isRunning)
    }

    @Test
    fun `engine ready moves to listening and recognitions update the result`() {
        val session = listening.onRecognized(hello)

        assertEquals(SessionStatus.Listening, session.status)
        assertEquals(hello, session.latest)
    }

    @Test
    fun `stopping keeps the last result but stops the engine`() {
        val session = listening.onRecognized(hello).onStopRequested()

        assertEquals(SessionStatus.Idle, session.status)
        assertFalse(session.isRunning)
        assertEquals(hello, session.latest)
    }

    @Test
    fun `results arriving after stop are ignored`() {
        val session = listening.onStopRequested().onRecognized(hello)

        assertNull(session.latest)
    }

    @Test
    fun `a new session clears the previous result`() {
        val session = listening.onRecognized(hello).onStopRequested()
            .onStartRequested(hasCameraPermission = true)

        assertNull(session.latest)
    }

    @Test
    fun `a failure stops the session and reports why`() {
        val session = listening.onFailure(RecognitionFailure.INFERENCE)

        assertEquals(SessionStatus.Failed(RecognitionFailure.INFERENCE), session.status)
        assertFalse(session.isRunning)
    }

    @Test
    fun `a late engine callback after stop does not restart listening`() {
        val session = TranslateSession().onStartRequested(hasCameraPermission = true)
            .onStopRequested()
            .onEngineReady()

        assertEquals(SessionStatus.Idle, session.status)
    }
}
