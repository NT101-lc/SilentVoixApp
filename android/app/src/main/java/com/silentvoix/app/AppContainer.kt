package com.silentvoix.app

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.silentvoix.app.domain.admin.AdminRepository
import com.silentvoix.app.domain.auth.AuthRepository
import com.silentvoix.app.domain.auth.SessionRepository
import com.silentvoix.app.domain.backend.BackendHealthRepository
import com.silentvoix.app.domain.feedback.FeedbackRepository
import com.silentvoix.app.domain.history.HistoryRepository
import com.silentvoix.app.domain.phrases.PhraseRepository
import com.silentvoix.app.domain.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope

/**
 * The app's dependencies, typed as the domain's repository interfaces so ViewModels never see
 * DataStore, Room or HTTP. [SilentVoixApplication] provides the real ones; tests pass fakes.
 */
interface AppContainer {
    /** For writes that must finish even if the screen that started them goes away. */
    val appScope: CoroutineScope
    val settingsRepository: SettingsRepository
    val phraseRepository: PhraseRepository
    val historyRepository: HistoryRepository
    val sessionRepository: SessionRepository
    val authRepository: AuthRepository
    val adminRepository: AdminRepository
    val feedbackRepository: FeedbackRepository
    val backendHealthRepository: BackendHealthRepository
}

/** The container inside a `viewModel { ... }` initializer. */
fun CreationExtras.appContainer(): AppContainer = this[APPLICATION_KEY] as AppContainer
