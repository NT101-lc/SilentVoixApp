package com.silentvoix.app.data.auth

import com.silentvoix.app.data.api.ApiError
import com.silentvoix.app.data.api.ApiResult
import org.junit.Assert.assertEquals
import org.junit.Test

/** What the app does with the answer to "who am I?" when it starts. */
class SessionRefreshTest {

    private val account = Account("id", "lan@example.com", "Lan", UserRole.USER)

    @Test
    fun `a fresh answer replaces the stored account`() {
        val promoted = account.copy(role = UserRole.ADMIN)

        assertEquals(SessionRefresh.Replace(promoted), SessionRefresh.from(ApiResult.Ok(promoted)))
    }

    @Test
    fun `an expired or revoked session signs out`() {
        assertEquals(SessionRefresh.SignOut, SessionRefresh.from(ApiResult.Failed(ApiError.UNAUTHORIZED)))
    }

    @Test
    fun `being offline keeps the session, so the app still works without a network`() {
        assertEquals(SessionRefresh.Keep, SessionRefresh.from(ApiResult.Failed(ApiError.NETWORK)))
        assertEquals(SessionRefresh.Keep, SessionRefresh.from(ApiResult.Failed(ApiError.SERVER_UNAVAILABLE)))
    }
}
