package com.silentvoix.app.data.api

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiErrorTest {

    @Test
    fun `the server's codes map to their own errors`() {
        assertEquals(ApiError.INVALID_CREDENTIALS, ApiError.from(401, "invalid_credentials"))
        assertEquals(ApiError.EMAIL_TAKEN, ApiError.from(409, "email_taken"))
        assertEquals(ApiError.ACCOUNT_DISABLED, ApiError.from(403, "account_disabled"))
        assertEquals(ApiError.TOO_MANY_ATTEMPTS, ApiError.from(429, "too_many_attempts"))
        assertEquals(ApiError.CANNOT_CHANGE_SELF, ApiError.from(409, "cannot_change_self"))
        assertEquals(ApiError.PASSWORD_TOO_SHORT, ApiError.from(400, "password_too_short"))
    }

    @Test
    fun `a database outage is the server being unavailable`() {
        assertEquals(ApiError.SERVER_UNAVAILABLE, ApiError.from(503, "database_unavailable"))
    }

    @Test
    fun `without a known code the status decides`() {
        assertEquals(ApiError.UNAUTHORIZED, ApiError.from(401, null))
        assertEquals(ApiError.FORBIDDEN, ApiError.from(403, "something_new"))
        assertEquals(ApiError.NOT_FOUND, ApiError.from(404, null))
        assertEquals(ApiError.SERVER_UNAVAILABLE, ApiError.from(502, null))
        assertEquals(ApiError.UNKNOWN, ApiError.from(418, null))
    }

    @Test
    fun `other input codes are invalid input`() {
        assertEquals(ApiError.INVALID_INPUT, ApiError.from(400, "invalid_kind"))
        assertEquals(ApiError.INVALID_INPUT, ApiError.from(400, "bad_request"))
    }
}
