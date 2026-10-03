package com.silentvoix.app.data.api

/** The outcome of one backend call. */
sealed interface ApiResult<out T> {
    data class Ok<T>(val value: T) : ApiResult<T>

    data class Failed(val error: ApiError) : ApiResult<Nothing>
}

/** Why a call failed, from the server's `{"error": "<code>"}` body or, failing that, the status. */
enum class ApiError {
    INVALID_CREDENTIALS,
    EMAIL_TAKEN,
    INVALID_EMAIL,
    PASSWORD_TOO_SHORT,
    PASSWORD_TOO_LONG,
    INVALID_DISPLAY_NAME,
    ACCOUNT_DISABLED,
    TOO_MANY_ATTEMPTS,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    CANNOT_CHANGE_SELF,
    INVALID_INPUT,
    SERVER_UNAVAILABLE,
    NETWORK,
    UNKNOWN;

    companion object {
        private val byCode = mapOf(
            "invalid_credentials" to INVALID_CREDENTIALS,
            "email_taken" to EMAIL_TAKEN,
            "invalid_email" to INVALID_EMAIL,
            "password_too_short" to PASSWORD_TOO_SHORT,
            "password_too_long" to PASSWORD_TOO_LONG,
            "invalid_display_name" to INVALID_DISPLAY_NAME,
            "account_disabled" to ACCOUNT_DISABLED,
            "too_many_attempts" to TOO_MANY_ATTEMPTS,
            "unauthorized" to UNAUTHORIZED,
            "forbidden" to FORBIDDEN,
            "not_found" to NOT_FOUND,
            "cannot_change_self" to CANNOT_CHANGE_SELF,
            "invalid_role" to INVALID_INPUT,
            "invalid_kind" to INVALID_INPUT,
            "invalid_message" to INVALID_INPUT,
            "invalid_device" to INVALID_INPUT,
            "bad_request" to INVALID_INPUT,
            "database_unavailable" to SERVER_UNAVAILABLE,
        )

        fun from(status: Int, code: String?): ApiError = byCode[code] ?: when {
            status == 401 -> UNAUTHORIZED
            status == 403 -> FORBIDDEN
            status == 404 -> NOT_FOUND
            status == 429 -> TOO_MANY_ATTEMPTS
            status >= 500 -> SERVER_UNAVAILABLE
            else -> UNKNOWN
        }
    }
}
