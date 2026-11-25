package com.silentvoix.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silentvoix.app.AppContainer
import com.silentvoix.app.domain.admin.FeedbackKind
import com.silentvoix.app.domain.auth.AuthRepository
import com.silentvoix.app.domain.auth.Session
import com.silentvoix.app.domain.auth.SessionRefresh
import com.silentvoix.app.domain.auth.SessionRepository
import com.silentvoix.app.domain.backend.BackendHealthRepository
import com.silentvoix.app.domain.backend.BackendStatus
import com.silentvoix.app.domain.common.ApiError
import com.silentvoix.app.domain.common.ApiResult
import com.silentvoix.app.domain.feedback.FeedbackRepository
import com.silentvoix.app.domain.history.HistoryRepository
import com.silentvoix.app.domain.settings.AppSettings
import com.silentvoix.app.domain.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The stored session once read: a session, or null when nobody is signed in. */
data class StoredSession(val session: Session?)

/** The Settings feedback form: whether a send is in flight, and how many went through. */
data class FeedbackFormState(val sending: Boolean = false, val sentCount: Int = 0)

/** One-off things the app shell shows as a snackbar. */
sealed interface AppEvent {
    data object FeedbackSent : AppEvent

    data class Failed(val error: ApiError) : AppEvent
}

/**
 * App-wide logic behind [SilentVoixApp]: settings, the session (sign-in, sign-out, "who am I?"),
 * recording recognised phrases, the feedback form and the backend's health. Talks only to the
 * domain's repository interfaces.
 */
class AppViewModel(
    private val settingsRepository: SettingsRepository,
    private val sessionRepository: SessionRepository,
    private val historyRepository: HistoryRepository,
    private val authRepository: AuthRepository,
    private val feedbackRepository: FeedbackRepository,
    private val backendHealth: BackendHealthRepository,
    private val appScope: CoroutineScope,
) : ViewModel() {

    constructor(container: AppContainer) : this(
        container.settingsRepository,
        container.sessionRepository,
        container.historyRepository,
        container.authRepository,
        container.feedbackRepository,
        container.backendHealthRepository,
        container.appScope,
    )

    /** Null until the stored settings are read, so a saved dark theme does not flash the default one. */
    val settings: StateFlow<AppSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Null until read, so "not read yet" does not flash the sign-in screen. */
    val session: StateFlow<StoredSession?> = sessionRepository.session
        .map { StoredSession(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _sessionExpired = MutableStateFlow(false)

    /** True when the sign-in screen shows because the session ended, not by the person's choice. */
    val sessionExpired: StateFlow<Boolean> = _sessionExpired.asStateFlow()

    private val _backendStatus = MutableStateFlow<BackendStatus>(BackendStatus.Checking)
    val backendStatus: StateFlow<BackendStatus> = _backendStatus.asStateFlow()
    private var healthCheck: Job? = null

    private val _feedbackForm = MutableStateFlow(FeedbackFormState())
    val feedbackForm: StateFlow<FeedbackFormState> = _feedbackForm.asStateFlow()

    private val _events = Channel<AppEvent>(Channel.BUFFERED)
    val events: Flow<AppEvent> = _events.receiveAsFlow()

    init {
        checkBackend()
    }

    /** Asks the backend how it is; a newer check replaces one still running. */
    fun checkBackend() {
        healthCheck?.cancel()
        healthCheck = viewModelScope.launch {
            _backendStatus.value = BackendStatus.Checking
            _backendStatus.value = backendHealth.fetchStatus()
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        appScope.launch { settingsRepository.update(transform) }
    }

    /** Every recognised phrase is saved to history. */
    fun onRecognized(text: String, confidencePercent: Int) {
        appScope.launch { historyRepository.add(text, confidencePercent) }
    }

    fun onSignedIn(session: Session) {
        _sessionExpired.value = false
        appScope.launch { sessionRepository.save(session) }
    }

    /** [expired]: the session ended on its own (the sign-in screen then says so). */
    fun signOut(expired: Boolean) {
        _sessionExpired.value = expired
        val token = session.value?.session?.token
        appScope.launch {
            // Best effort: the server forgets the token if it can be reached; this device forgets it regardless.
            token?.let { authRepository.logout(it) }
            sessionRepository.clear()
        }
    }

    /** Asks the server who this is: on start, and whenever an admin call is refused (role taken away). */
    fun refreshAccount() {
        val token = session.value?.session?.token ?: return
        viewModelScope.launch {
            when (val refresh = SessionRefresh.from(authRepository.me(token))) {
                is SessionRefresh.Replace -> sessionRepository.updateAccount(refresh.account)
                SessionRefresh.SignOut -> signOut(expired = true)
                // Offline or the server is down: keep working with the stored account.
                SessionRefresh.Keep -> Unit
            }
        }
    }

    fun sendFeedback(kind: FeedbackKind, message: String) {
        val token = session.value?.session?.token ?: return
        if (_feedbackForm.value.sending) return
        _feedbackForm.update { it.copy(sending = true) }
        viewModelScope.launch {
            val result = feedbackRepository.sendFeedback(token, kind, message)
            _feedbackForm.update { it.copy(sending = false) }
            when (result) {
                is ApiResult.Ok -> {
                    _feedbackForm.update { it.copy(sentCount = it.sentCount + 1) }
                    _events.send(AppEvent.FeedbackSent)
                }
                is ApiResult.Failed -> if (result.error == ApiError.UNAUTHORIZED) {
                    signOut(expired = true)
                } else {
                    _events.send(AppEvent.Failed(result.error))
                }
            }
        }
    }
}
