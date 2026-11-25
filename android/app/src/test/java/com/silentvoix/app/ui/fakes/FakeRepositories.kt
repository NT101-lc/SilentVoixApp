package com.silentvoix.app.ui.fakes

import com.silentvoix.app.domain.admin.AdminOverview
import com.silentvoix.app.domain.admin.AdminRepository
import com.silentvoix.app.domain.admin.FeedbackItem
import com.silentvoix.app.domain.admin.FeedbackKind
import com.silentvoix.app.domain.admin.ManagedUser
import com.silentvoix.app.domain.auth.Account
import com.silentvoix.app.domain.auth.AuthRepository
import com.silentvoix.app.domain.auth.Session
import com.silentvoix.app.domain.auth.SessionRepository
import com.silentvoix.app.domain.auth.UserRole
import com.silentvoix.app.domain.common.ApiResult
import com.silentvoix.app.domain.feedback.FeedbackRepository
import com.silentvoix.app.domain.history.HistoryEntry
import com.silentvoix.app.domain.history.HistoryRepository
import com.silentvoix.app.domain.settings.AppSettings
import com.silentvoix.app.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

val userAccount = Account("u1", "lan@example.com", "Lan", UserRole.USER)
val adminAccount = Account("a1", "admin@example.com", "Quản trị", UserRole.ADMIN)

class FakeSettings : SettingsRepository {
    val current = MutableStateFlow(AppSettings())
    override val settings: Flow<AppSettings> = current

    override suspend fun update(transform: (AppSettings) -> AppSettings) = current.update(transform)
}

class FakeSessions(initial: Session? = null) : SessionRepository {
    val current = MutableStateFlow(initial)
    override val session: Flow<Session?> = current

    override suspend fun save(session: Session) {
        current.value = session
    }

    override suspend fun updateAccount(account: Account) = current.update { it?.copy(account = account) }

    override suspend fun clear() {
        current.value = null
    }
}

class FakeHistory : HistoryRepository {
    val current = MutableStateFlow<List<HistoryEntry>>(emptyList())
    override val entries: Flow<List<HistoryEntry>> = current

    override suspend fun add(text: String, confidencePercent: Int) = current.update {
        listOf(HistoryEntry(it.size + 1L, text, confidencePercent, 0L, false)) + it
    }

    override suspend fun setFavourite(id: Long, favourite: Boolean) = current.update { entries ->
        entries.map { if (it.id == id) it.copy(isFavourite = favourite) else it }
    }

    override suspend fun clear() {
        current.value = emptyList()
    }
}

/** Answers with whatever the test sets; records what was asked. */
class FakeAuth : AuthRepository {
    var loginResult: ApiResult<Session> = ApiResult.Ok(Session("token", userAccount))
    var meResult: ApiResult<Account> = ApiResult.Ok(userAccount)
    val calls = mutableListOf<String>()

    override suspend fun login(email: String, password: String): ApiResult<Session> {
        calls += "login $email"
        return loginResult
    }

    override suspend fun register(email: String, password: String, displayName: String?): ApiResult<Session> {
        calls += "register $email $displayName"
        return loginResult
    }

    override suspend fun me(token: String): ApiResult<Account> {
        calls += "me $token"
        return meResult
    }

    override suspend fun logout(token: String): ApiResult<Unit> {
        calls += "logout $token"
        return ApiResult.Ok(Unit)
    }
}

class FakeFeedback : FeedbackRepository {
    var result: ApiResult<Unit> = ApiResult.Ok(Unit)
    val sent = mutableListOf<Pair<FeedbackKind, String>>()

    override suspend fun sendFeedback(token: String, kind: FeedbackKind, message: String): ApiResult<Unit> {
        sent += kind to message
        return result
    }
}

class FakeAdmin : AdminRepository {
    var overviewResult: ApiResult<AdminOverview> = ApiResult.Ok(AdminOverview(2, 1, 0, 0, 1, 1, 1))
    var usersResult: ApiResult<List<ManagedUser>> = ApiResult.Ok(listOf(managedUser("u1"), managedUser("u2")))
    var feedbackResult: ApiResult<List<FeedbackItem>> = ApiResult.Ok(listOf(feedbackItem("f1")))
    var updateResult: ((String) -> ApiResult<ManagedUser>) = { id -> ApiResult.Ok(managedUser(id, locked = true)) }
    val feedbackRequests = mutableListOf<Boolean>()

    override suspend fun overview(token: String) = overviewResult

    override suspend fun users(token: String) = usersResult

    override suspend fun updateUser(token: String, id: String, role: UserRole?, locked: Boolean?) = updateResult(id)

    override suspend fun feedback(token: String, openOnly: Boolean): ApiResult<List<FeedbackItem>> {
        feedbackRequests += openOnly
        return feedbackResult
    }

    override suspend fun setResolved(token: String, id: String, resolved: Boolean): ApiResult<FeedbackItem> =
        ApiResult.Ok(feedbackItem(id, resolvedAtMillis = if (resolved) 1L else null))
}

fun managedUser(id: String, locked: Boolean = false) =
    ManagedUser(id, "$id@example.com", null, UserRole.USER, locked, createdAtMillis = 0L, lastSeenAtMillis = null)

fun feedbackItem(id: String, resolvedAtMillis: Long? = null) =
    FeedbackItem(id, FeedbackKind.BUG, "Lỗi", createdAtMillis = 0L, resolvedAtMillis = resolvedAtMillis, authorEmail = null, authorName = null)
