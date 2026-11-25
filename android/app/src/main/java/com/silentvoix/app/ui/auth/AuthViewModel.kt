package com.silentvoix.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silentvoix.app.domain.auth.AuthRepository
import com.silentvoix.app.domain.auth.FormErrors
import com.silentvoix.app.domain.auth.Session
import com.silentvoix.app.domain.auth.normalizeEmail
import com.silentvoix.app.domain.auth.validateRegister
import com.silentvoix.app.domain.auth.validateSignIn
import com.silentvoix.app.domain.common.ApiError
import com.silentvoix.app.domain.common.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

data class AuthUiState(
    val mode: AuthMode = AuthMode.SIGN_IN,
    val form: AuthForm = AuthForm(),
    /** Field errors show only after a first attempt, not while someone is still typing. */
    val attempted: Boolean = false,
    val submitting: Boolean = false,
    val serverError: ApiError? = null,
) {
    val errors: FormErrors
        get() = when (mode) {
            AuthMode.SIGN_IN -> validateSignIn(form.email, form.password)
            AuthMode.REGISTER -> validateRegister(form.email, form.password, form.confirm, form.name)
        }

    val shownErrors: FormErrors get() = if (attempted) errors else FormErrors()
}

/** Sign in, or create an ordinary account (admins are made by another admin). */
class AuthViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun setMode(mode: AuthMode) {
        _state.update { it.copy(mode = mode, attempted = false, serverError = null) }
    }

    fun updateForm(form: AuthForm) {
        _state.update { it.copy(form = form, serverError = null) }
    }

    /** Sends the form once it is valid; [onSignedIn] gets the new session, and the form is cleared. */
    fun submit(onSignedIn: (Session) -> Unit) {
        val current = _state.updateAndGet { it.copy(attempted = true) }
        if (!current.errors.isValid || current.submitting) return
        _state.update { it.copy(submitting = true, serverError = null) }
        viewModelScope.launch {
            val form = current.form
            val address = normalizeEmail(form.email)
            val result = when (current.mode) {
                AuthMode.SIGN_IN -> auth.login(address, form.password)
                AuthMode.REGISTER -> auth.register(address, form.password, form.name.trim().ifEmpty { null })
            }
            when (result) {
                is ApiResult.Ok -> {
                    // Whoever signs out next finds an empty form, not this person's password.
                    _state.value = AuthUiState()
                    onSignedIn(result.value)
                }
                is ApiResult.Failed -> _state.update { it.copy(submitting = false, serverError = result.error) }
            }
        }
    }
}
