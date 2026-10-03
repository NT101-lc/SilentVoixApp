package com.silentvoix.app.data.admin

import com.silentvoix.app.data.auth.UserRole
import java.text.Normalizer

/** Counts for the admin overview. "This week" is the last seven days. */
data class AdminOverview(
    val users: Int,
    val admins: Int,
    val lockedUsers: Int,
    val newUsersThisWeek: Int,
    val activeUsersThisWeek: Int,
    val openFeedback: Int,
    val totalFeedback: Int,
)

/** An account as an admin sees it. */
data class ManagedUser(
    val id: String,
    val email: String?,
    val displayName: String?,
    val role: UserRole,
    val locked: Boolean,
    val createdAtMillis: Long,
    /** The last time any of their devices called the server; null if never. */
    val lastSeenAtMillis: Long?,
)

/** What a piece of feedback is about; the server's names are [apiValue]. */
enum class FeedbackKind(val apiValue: String) {
    WRONG_RESULT("wrong_result"),
    BUG("bug"),
    IDEA("idea");

    companion object {
        fun fromApi(value: String?): FeedbackKind? = entries.firstOrNull { it.apiValue == value }
    }
}

data class FeedbackItem(
    val id: String,
    /** Null for a kind this version of the app does not know. */
    val kind: FeedbackKind?,
    val message: String,
    val createdAtMillis: Long,
    val resolvedAtMillis: Long?,
    val authorEmail: String?,
    val authorName: String?,
) {
    val isResolved: Boolean get() = resolvedAtMillis != null
}

/** Users whose e-mail or name contains [query], ignoring case and Vietnamese marks. */
fun List<ManagedUser>.matching(query: String): List<ManagedUser> {
    val needle = foldForSearch(query.trim())
    if (needle.isEmpty()) return this
    return filter { user ->
        listOfNotNull(user.email, user.displayName).any { foldForSearch(it).contains(needle) }
    }
}

private val COMBINING_MARKS = Regex("\\p{Mn}+")

/** "Đặng Thị" -> "dang thi". */
internal fun foldForSearch(text: String): String =
    Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
        .replace(COMBINING_MARKS, "")
        .replace('đ', 'd')
