package com.silentvoix.app.ui.auth

import com.silentvoix.app.domain.auth.FormError
import com.silentvoix.app.domain.auth.Session
import com.silentvoix.app.domain.common.ApiError
import com.silentvoix.app.domain.common.ApiResult
import com.silentvoix.app.ui.fakes.FakeAuth
import com.silentvoix.app.ui.fakes.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AuthViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val auth = FakeAuth()
    private val viewModel = AuthViewModel(auth)

    @Test
    fun errorsShowOnlyAfterAFirstAttempt() {
        viewModel.updateForm(AuthForm(email = "not-an-address"))
        assertTrue(viewModel.state.value.shownErrors.isValid)

        viewModel.submit { }

        assertEquals(FormError.EMAIL_INVALID, viewModel.state.value.shownErrors.email)
        assertTrue(auth.calls.isEmpty())
    }

    @Test
    fun signingInSendsTheNormalisedAddressAndClearsTheForm() {
        var signedIn: Session? = null
        viewModel.updateForm(AuthForm(email = " Lan@Example.COM ", password = "secret-pass"))

        viewModel.submit { signedIn = it }

        assertEquals(listOf("login lan@example.com"), auth.calls)
        assertEquals("token", signedIn?.token)
        assertEquals(AuthUiState(), viewModel.state.value)
    }

    @Test
    fun aRefusalIsShownAndClearedByTyping() {
        auth.loginResult = ApiResult.Failed(ApiError.INVALID_CREDENTIALS)
        viewModel.updateForm(AuthForm(email = "lan@example.com", password = "wrong-pass"))

        viewModel.submit { }
        assertEquals(ApiError.INVALID_CREDENTIALS, viewModel.state.value.serverError)

        viewModel.updateForm(viewModel.state.value.form.copy(password = "other-pass"))
        assertNull(viewModel.state.value.serverError)
    }

    @Test
    fun registeringSendsNoNameWhenItIsBlank() {
        viewModel.setMode(AuthMode.REGISTER)
        viewModel.updateForm(
            AuthForm(email = "lan@example.com", password = "long-enough", confirm = "long-enough", name = "  "),
        )

        viewModel.submit { }

        assertEquals(listOf("register lan@example.com null"), auth.calls)
    }
}
