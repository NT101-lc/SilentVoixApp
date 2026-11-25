package com.silentvoix.app.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silentvoix.app.domain.admin.AdminOverview
import com.silentvoix.app.domain.admin.AdminRepository
import com.silentvoix.app.domain.admin.FeedbackItem
import com.silentvoix.app.domain.admin.ManagedUser
import com.silentvoix.app.domain.auth.UserRole
import com.silentvoix.app.domain.common.ApiError
import com.silentvoix.app.domain.common.ApiResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Something fetched from the server: still coming, failed, or here. */
sealed interface Loadable<out T> {
    data object Loading : Loadable<Nothing>

    data class Failed(val error: ApiError) : Loadable<Nothing>

    data class Loaded<T>(val value: T) : Loadable<T>
}

/** Account actions an admin can take from a user's menu. */
enum class UserAction { MAKE_ADMIN, REMOVE_ADMIN, LOCK, UNLOCK }

data class AdminUiState(
    val overview: Loadable<AdminOverview> = Loadable.Loading,
    val users: Loadable<List<ManagedUser>> = Loadable.Loading,
    val feedback: Loadable<List<FeedbackItem>> = Loadable.Loading,
    val feedbackOpenOnly: Boolean = true,
    /** Rows with a change on its way to the server. */
    val busyIds: Set<String> = emptySet(),
)

/** What the admin screen reports to the app, or shows as a message. */
sealed interface AdminEvent {
    /** The session is gone (401). */
    data object Unauthorized : AdminEvent

    /** This person is no longer an admin (403). */
    data object Forbidden : AdminEvent

    data class UserUpdated(val user: ManagedUser) : AdminEvent

    data class Failed(val error: ApiError) : AdminEvent
}

/**
 * The admin tab for one session ([token]). Everything comes from the server, which re-checks the
 * role on every call; a 401 or 403 is reported once as an [AdminEvent].
 */
class AdminViewModel(
    private val admin: AdminRepository,
    private val token: String,
) : ViewModel() {

    private val _state = MutableStateFlow(AdminUiState())
    val state: StateFlow<AdminUiState> = _state.asStateFlow()

    private val _events = Channel<AdminEvent>(Channel.BUFFERED)
    val events: Flow<AdminEvent> = _events.receiveAsFlow()

    private var summaryLoad: Job? = null
    private var feedbackLoad: Job? = null

    /** Reloads everything: the overview, the accounts and the feedback inbox. */
    fun refresh() {
        summaryLoad?.cancel()
        summaryLoad = viewModelScope.launch {
            val overview = async { admin.overview(token).checked().toLoadable() }
            val users = async { admin.users(token).checked().toLoadable() }
            _state.update { it.copy(overview = overview.await(), users = users.await()) }
        }
        loadFeedback()
    }

    fun setFeedbackOpenOnly(openOnly: Boolean) {
        if (openOnly == _state.value.feedbackOpenOnly) return
        _state.update { it.copy(feedbackOpenOnly = openOnly) }
        loadFeedback()
    }

    fun onUserAction(user: ManagedUser, action: UserAction) {
        setBusy(user.id, true)
        viewModelScope.launch {
            val result = when (action) {
                UserAction.MAKE_ADMIN -> admin.updateUser(token, user.id, role = UserRole.ADMIN)
                UserAction.REMOVE_ADMIN -> admin.updateUser(token, user.id, role = UserRole.USER)
                UserAction.LOCK -> admin.updateUser(token, user.id, locked = true)
                UserAction.UNLOCK -> admin.updateUser(token, user.id, locked = false)
            }.checked()
            setBusy(user.id, false)
            when (result) {
                is ApiResult.Ok -> {
                    _state.update { state ->
                        val users = state.users
                        if (users !is Loadable.Loaded) return@update state
                        state.copy(users = Loadable.Loaded(users.value.map { if (it.id == user.id) result.value else it }))
                    }
                    _events.send(AdminEvent.UserUpdated(result.value))
                    reloadOverview()
                }
                is ApiResult.Failed -> _events.send(AdminEvent.Failed(result.error))
            }
        }
    }

    fun onToggleResolved(item: FeedbackItem) {
        setBusy(item.id, true)
        viewModelScope.launch {
            val result = admin.setResolved(token, item.id, !item.isResolved).checked()
            setBusy(item.id, false)
            when (result) {
                is ApiResult.Ok -> {
                    _state.update { state ->
                        val feedback = state.feedback
                        if (feedback !is Loadable.Loaded) return@update state
                        state.copy(
                            feedback = Loadable.Loaded(
                                feedback.value
                                    .map { if (it.id == item.id) result.value else it }
                                    // In the open inbox a handled item leaves at once.
                                    .filter { !state.feedbackOpenOnly || !it.isResolved },
                            ),
                        )
                    }
                    reloadOverview()
                }
                is ApiResult.Failed -> _events.send(AdminEvent.Failed(result.error))
            }
        }
    }

    private fun loadFeedback() {
        feedbackLoad?.cancel()
        val openOnly = _state.value.feedbackOpenOnly
        _state.update { it.copy(feedback = Loadable.Loading) }
        feedbackLoad = viewModelScope.launch {
            val feedback = admin.feedback(token, openOnly).checked().toLoadable()
            _state.update { it.copy(feedback = feedback) }
        }
    }

    private suspend fun reloadOverview() {
        val overview = admin.overview(token).checked().toLoadable()
        _state.update { it.copy(overview = overview) }
    }

    private fun setBusy(id: String, busy: Boolean) {
        _state.update { it.copy(busyIds = if (busy) it.busyIds + id else it.busyIds - id) }
    }

    /** Any failure that means "not allowed any more" is reported once, here. */
    private suspend fun <T> ApiResult<T>.checked(): ApiResult<T> {
        if (this is ApiResult.Failed) {
            when (error) {
                ApiError.UNAUTHORIZED -> _events.send(AdminEvent.Unauthorized)
                ApiError.FORBIDDEN -> _events.send(AdminEvent.Forbidden)
                else -> Unit
            }
        }
        return this
    }

    private fun <T> ApiResult<T>.toLoadable(): Loadable<T> = when (this) {
        is ApiResult.Ok -> Loadable.Loaded(value)
        is ApiResult.Failed -> Loadable.Failed(error)
    }
}
