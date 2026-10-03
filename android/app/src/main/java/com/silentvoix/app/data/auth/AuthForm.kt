package com.silentvoix.app.data.auth

import java.util.Locale

/** Why a field of the sign-in or sign-up form cannot be sent yet. */
enum class FormError { EMAIL_INVALID, PASSWORD_MISSING, PASSWORD_TOO_SHORT, PASSWORD_TOO_LONG, PASSWORDS_DIFFER, NAME_TOO_LONG }

/** One error (or none) per field. */
data class FormErrors(
    val email: FormError? = null,
    val password: FormError? = null,
    val confirm: FormError? = null,
    val name: FormError? = null,
) {
    val isValid: Boolean get() = email == null && password == null && confirm == null && name == null
}

// Same limits as the backend (auth/Credentials.java).
const val MIN_PASSWORD_LENGTH = 8
private const val MAX_PASSWORD_BYTES = 72
private const val MAX_NAME_LENGTH = 80
private val EMAIL = Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")

/** Trimmed and in lower case, as the server stores it. */
fun normalizeEmail(raw: String): String = raw.trim().lowercase(Locale.ROOT)

fun validateSignIn(email: String, password: String) = FormErrors(
    email = emailError(email),
    // An existing password is the server's to judge; only an empty one is caught here.
    password = if (password.isEmpty()) FormError.PASSWORD_MISSING else null,
)

fun validateRegister(email: String, password: String, confirm: String, name: String) = FormErrors(
    email = emailError(email),
    password = when {
        password.codePointCount(0, password.length) < MIN_PASSWORD_LENGTH -> FormError.PASSWORD_TOO_SHORT
        password.toByteArray(Charsets.UTF_8).size > MAX_PASSWORD_BYTES -> FormError.PASSWORD_TOO_LONG
        else -> null
    },
    confirm = if (confirm != password) FormError.PASSWORDS_DIFFER else null,
    name = name.trim().let { if (it.codePointCount(0, it.length) > MAX_NAME_LENGTH) FormError.NAME_TOO_LONG else null },
)

private fun emailError(raw: String): FormError? =
    if (EMAIL.matches(normalizeEmail(raw))) null else FormError.EMAIL_INVALID
