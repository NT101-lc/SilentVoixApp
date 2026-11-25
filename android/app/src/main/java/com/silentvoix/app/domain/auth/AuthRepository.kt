package com.silentvoix.app.domain.auth

import com.silentvoix.app.domain.common.ApiResult

/** Accounts on the backend: sign in, sign up, who am I, sign out. */
interface AuthRepository {
    suspend fun login(email: String, password: String): ApiResult<Session>

    suspend fun register(email: String, password: String, displayName: String?): ApiResult<Session>

    suspend fun me(token: String): ApiResult<Account>

    suspend fun logout(token: String): ApiResult<Unit>
}
