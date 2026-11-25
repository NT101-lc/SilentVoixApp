package com.silentvoix.app.domain.feedback

import com.silentvoix.app.domain.admin.FeedbackKind
import com.silentvoix.app.domain.common.ApiResult

/** Sends a wrong result, a bug or an idea to the backend. */
interface FeedbackRepository {
    suspend fun sendFeedback(token: String, kind: FeedbackKind, message: String): ApiResult<Unit>
}
