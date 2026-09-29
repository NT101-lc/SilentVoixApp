package com.silentvoix.app.recognition

/** A phrase recognised from the camera. */
data class Recognition(val text: String, val confidencePercent: Int)

enum class RecognitionFailure { CAMERA_UNAVAILABLE, MODEL_UNAVAILABLE, INFERENCE }

sealed interface SessionStatus {
    data object Idle : SessionStatus
    data object AwaitingPermission : SessionStatus
    data object PermissionDenied : SessionStatus

    /** Camera is opening and the model is loading. */
    data object Starting : SessionStatus
    data object Listening : SessionStatus
    data class Failed(val failure: RecognitionFailure) : SessionStatus
}

/**
 * State of the Translate screen's recognition session. Immutable: every event returns the next
 * state, and events that do not apply to the current state (a late model callback after the user
 * pressed stop, say) leave it unchanged.
 */
data class TranslateSession(
    val status: SessionStatus = SessionStatus.Idle,
    val latest: Recognition? = null,
    val hasRunOnce: Boolean = false,
    /** Bumped on every recognition, so repeating the same phrase still reads as a new result. */
    val resultCount: Int = 0,
) {
    /** Whether the camera and model should be running. */
    val isRunning: Boolean
        get() = status == SessionStatus.Starting || status == SessionStatus.Listening

    fun onStartRequested(hasCameraPermission: Boolean): TranslateSession = copy(
        status = if (hasCameraPermission) SessionStatus.Starting else SessionStatus.AwaitingPermission,
        latest = null,
        hasRunOnce = true,
    )

    fun onPermissionResult(granted: Boolean): TranslateSession {
        if (status != SessionStatus.AwaitingPermission) return this
        return copy(status = if (granted) SessionStatus.Starting else SessionStatus.PermissionDenied)
    }

    fun onEngineReady(): TranslateSession =
        if (status == SessionStatus.Starting) copy(status = SessionStatus.Listening) else this

    fun onRecognized(recognition: Recognition): TranslateSession =
        if (status == SessionStatus.Listening) {
            copy(latest = recognition, resultCount = resultCount + 1)
        } else {
            this
        }

    fun onFailure(failure: RecognitionFailure): TranslateSession =
        if (isRunning) copy(status = SessionStatus.Failed(failure)) else this

    fun onStopRequested(): TranslateSession = copy(status = SessionStatus.Idle)
}
