package com.silentvoix.app.data.auth

import com.silentvoix.app.data.api.ApiError
import com.silentvoix.app.data.api.ApiResult

/** What to do with the stored session once the server has answered "who am I?". */
sealed interface SessionRefresh {
    /** Signed in: store the server's view (the role may have changed). */
    data class Replace(val account: Account) : SessionRefresh

    /** The session expired, was signed out elsewhere, or the account was locked. */
    data object SignOut : SessionRefresh

    /** No answer (offline, server down): keep working with what is stored. */
    data object Keep : SessionRefresh

    companion object {
        fun from(result: ApiResult<Account>): SessionRefresh = when (result) {
            is ApiResult.Ok -> Replace(result.value)
            is ApiResult.Failed -> if (result.error == ApiError.UNAUTHORIZED) SignOut else Keep
        }
    }
}
