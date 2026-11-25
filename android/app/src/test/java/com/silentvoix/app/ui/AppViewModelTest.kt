package com.silentvoix.app.ui

import com.silentvoix.app.domain.admin.FeedbackKind
import com.silentvoix.app.domain.auth.Session
import com.silentvoix.app.domain.backend.BackendHealthRepository
import com.silentvoix.app.domain.backend.BackendStatus
import com.silentvoix.app.domain.backend.DatabaseState
import com.silentvoix.app.domain.common.ApiError
import com.silentvoix.app.domain.common.ApiResult
import com.silentvoix.app.ui.fakes.FakeAuth
import com.silentvoix.app.ui.fakes.FakeFeedback
import com.silentvoix.app.ui.fakes.FakeHistory
import com.silentvoix.app.ui.fakes.FakeSessions
import com.silentvoix.app.ui.fakes.FakeSettings
import com.silentvoix.app.ui.fakes.MainDispatcherRule
import com.silentvoix.app.ui.fakes.adminAccount
import com.silentvoix.app.ui.fakes.userAccount
import kotlinx.coroutines.CoroutineScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val settings = FakeSettings()
    private val sessions = FakeSessions(Session("token", userAccount))
    private val history = FakeHistory()
    private val auth = FakeAuth()
    private val feedback = FakeFeedback()

    private fun viewModel() = AppViewModel(
        settingsRepository = settings,
        sessionRepository = sessions,
        historyRepository = history,
        authRepository = auth,
        feedbackRepository = feedback,
        backendHealth = object : BackendHealthRepository {
            override suspend fun fetchStatus() = BackendStatus.Online(DatabaseState.UP)
        },
        appScope = CoroutineScope(main.dispatcher),
    )

    @Test
    fun readsSettingsSessionAndBackendOnStart() {
        val app = viewModel()

        assertEquals(settings.current.value, app.settings.value)
        assertEquals(userAccount, app.session.value?.session?.account)
        assertEquals(BackendStatus.Online(DatabaseState.UP), app.backendStatus.value)
    }

    @Test
    fun aRoleChangeOnTheServerReplacesTheStoredAccount() {
        auth.meResult = ApiResult.Ok(adminAccount)
        val app = viewModel()

        app.refreshAccount()

        assertEquals(adminAccount, sessions.current.value?.account)
    }

    @Test
    fun anExpiredSessionSignsOutAndSaysWhy() {
        auth.meResult = ApiResult.Failed(ApiError.UNAUTHORIZED)
        val app = viewModel()

        app.refreshAccount()

        assertNull(sessions.current.value)
        assertTrue(app.sessionExpired.value)
        assertTrue("logout token" in auth.calls)
    }

    @Test
    fun beingOfflineKeepsTheStoredAccount() {
        auth.meResult = ApiResult.Failed(ApiError.NETWORK)
        val app = viewModel()

        app.refreshAccount()

        assertEquals(userAccount, sessions.current.value?.account)
    }

    @Test
    fun signingInAgainClearsTheExpiredNotice() {
        auth.meResult = ApiResult.Failed(ApiError.UNAUTHORIZED)
        val app = viewModel()
        app.refreshAccount()

        app.onSignedIn(Session("new", userAccount))

        assertFalse(app.sessionExpired.value)
        assertEquals("new", sessions.current.value?.token)
    }

    @Test
    fun sentFeedbackIsCounted() {
        val app = viewModel()

        app.sendFeedback(FeedbackKind.IDEA, "Thêm cử chỉ")

        assertEquals(listOf(FeedbackKind.IDEA to "Thêm cử chỉ"), feedback.sent)
        assertEquals(FeedbackFormState(sending = false, sentCount = 1), app.feedbackForm.value)
    }

    @Test
    fun feedbackRefusedForAnExpiredSessionSignsOut() {
        feedback.result = ApiResult.Failed(ApiError.UNAUTHORIZED)
        val app = viewModel()

        app.sendFeedback(FeedbackKind.BUG, "Lỗi")

        assertNull(sessions.current.value)
        assertEquals(0, app.feedbackForm.value.sentCount)
    }

    @Test
    fun recognisedPhrasesAreSavedToHistory() {
        viewModel().onRecognized("Xin chào", 91)

        assertEquals("Xin chào", history.current.value.single().text)
    }
}
