package com.silentvoix.app.domain.admin

import com.silentvoix.app.domain.auth.UserRole
import com.silentvoix.app.domain.common.ApiResult

/** The backend's admin endpoints. The server refuses them (403) for anyone but an admin. */
interface AdminRepository {
    suspend fun overview(token: String): ApiResult<AdminOverview>

    suspend fun users(token: String): ApiResult<List<ManagedUser>>

    /** Changes only the fields given. */
    suspend fun updateUser(token: String, id: String, role: UserRole? = null, locked: Boolean? = null): ApiResult<ManagedUser>

    suspend fun feedback(token: String, openOnly: Boolean): ApiResult<List<FeedbackItem>>

    suspend fun setResolved(token: String, id: String, resolved: Boolean): ApiResult<FeedbackItem>
}
