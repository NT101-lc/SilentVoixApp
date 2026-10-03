package com.silentvoix.app.ui.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.silentvoix.app.R
import com.silentvoix.app.data.api.ApiError
import com.silentvoix.app.data.auth.FormError

/** A sentence for the person, never the server's code. */
@Composable
fun apiErrorMessage(error: ApiError): String = stringResource(apiErrorRes(error))

@StringRes
fun apiErrorRes(error: ApiError): Int = when (error) {
    ApiError.INVALID_CREDENTIALS -> R.string.api_error_invalid_credentials
    ApiError.EMAIL_TAKEN -> R.string.api_error_email_taken
    ApiError.INVALID_EMAIL -> R.string.form_error_email_invalid
    ApiError.PASSWORD_TOO_SHORT -> R.string.form_error_password_too_short
    ApiError.PASSWORD_TOO_LONG -> R.string.form_error_password_too_long
    ApiError.INVALID_DISPLAY_NAME -> R.string.form_error_name_too_long
    ApiError.ACCOUNT_DISABLED -> R.string.api_error_account_disabled
    ApiError.TOO_MANY_ATTEMPTS -> R.string.api_error_too_many_attempts
    ApiError.UNAUTHORIZED -> R.string.session_expired
    ApiError.FORBIDDEN -> R.string.api_error_forbidden
    ApiError.NOT_FOUND -> R.string.api_error_not_found
    ApiError.CANNOT_CHANGE_SELF -> R.string.api_error_cannot_change_self
    ApiError.SERVER_UNAVAILABLE -> R.string.api_error_server_unavailable
    ApiError.NETWORK -> R.string.api_error_network
    ApiError.INVALID_INPUT, ApiError.UNKNOWN -> R.string.api_error_generic
}

@Composable
fun formErrorMessage(error: FormError): String = stringResource(
    when (error) {
        FormError.EMAIL_INVALID -> R.string.form_error_email_invalid
        FormError.PASSWORD_MISSING -> R.string.form_error_password_missing
        FormError.PASSWORD_TOO_SHORT -> R.string.form_error_password_too_short
        FormError.PASSWORD_TOO_LONG -> R.string.form_error_password_too_long
        FormError.PASSWORDS_DIFFER -> R.string.form_error_passwords_differ
        FormError.NAME_TOO_LONG -> R.string.form_error_name_too_long
    },
)
