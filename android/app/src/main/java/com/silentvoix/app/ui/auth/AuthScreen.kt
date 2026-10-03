package com.silentvoix.app.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.silentvoix.app.R
import com.silentvoix.app.data.api.ApiError
import com.silentvoix.app.data.api.ApiResult
import com.silentvoix.app.data.api.SilentVoixApi
import com.silentvoix.app.data.auth.FormErrors
import com.silentvoix.app.data.auth.Session
import com.silentvoix.app.data.auth.normalizeEmail
import com.silentvoix.app.data.auth.validateRegister
import com.silentvoix.app.data.auth.validateSignIn
import com.silentvoix.app.ui.common.SegmentedControl
import com.silentvoix.app.ui.common.apiErrorMessage
import com.silentvoix.app.ui.common.formErrorMessage
import com.silentvoix.app.ui.common.pullUp
import com.silentvoix.app.ui.scene.SkyScene
import com.silentvoix.app.ui.scene.TimeOfDay
import com.silentvoix.app.ui.scene.skyPalette
import java.time.LocalTime
import kotlinx.coroutines.launch

enum class AuthMode { SIGN_IN, REGISTER }

/** What the person has typed. Kept across rotation; the passwords too, so a turn does not wipe them. */
data class AuthForm(
    val email: String = "",
    val password: String = "",
    val confirm: String = "",
    val name: String = "",
)

/**
 * The way in: sign in, or create an ordinary account (admins are made by another admin). Shown
 * instead of the app whenever nobody is signed in. [notice] explains why, e.g. an expired session.
 */
@Composable
fun AuthScreen(api: SilentVoixApi, onSignedIn: (Session) -> Unit, notice: String? = null) {
    var mode by rememberSaveable { mutableStateOf(AuthMode.SIGN_IN) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    // Field errors show only after a first attempt, not while someone is still typing.
    var attempted by rememberSaveable { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var serverError by remember { mutableStateOf<ApiError?>(null) }
    val scope = rememberCoroutineScope()

    val form = AuthForm(email, password, confirm, name)
    val errors = validate(mode, form)
    AuthContent(
        mode = mode,
        onModeChange = {
            mode = it
            attempted = false
            serverError = null
        },
        form = form,
        onFormChange = {
            email = it.email
            password = it.password
            confirm = it.confirm
            name = it.name
            serverError = null
        },
        errors = if (attempted) errors else FormErrors(),
        serverError = serverError?.let { apiErrorMessage(it) } ?: notice,
        submitting = submitting,
        onSubmit = {
            attempted = true
            if (errors.isValid && !submitting) {
                submitting = true
                serverError = null
                scope.launch {
                    val address = normalizeEmail(email)
                    val result = when (mode) {
                        AuthMode.SIGN_IN -> api.login(address, password)
                        AuthMode.REGISTER -> api.register(address, password, name.trim().ifEmpty { null })
                    }
                    submitting = false
                    when (result) {
                        is ApiResult.Ok -> onSignedIn(result.value)
                        is ApiResult.Failed -> serverError = result.error
                    }
                }
            }
        },
    )
}

private fun validate(mode: AuthMode, form: AuthForm): FormErrors = when (mode) {
    AuthMode.SIGN_IN -> validateSignIn(form.email, form.password)
    AuthMode.REGISTER -> validateRegister(form.email, form.password, form.confirm, form.name)
}

/**
 * The painted countryside for the time of day, the app's name written on its sky, and a paper card
 * resting on the meadow with the form.
 */
@Composable
fun AuthContent(
    mode: AuthMode,
    onModeChange: (AuthMode) -> Unit,
    form: AuthForm,
    onFormChange: (AuthForm) -> Unit,
    errors: FormErrors,
    serverError: String?,
    submitting: Boolean,
    onSubmit: () -> Unit,
    time: TimeOfDay = TimeOfDay.from(LocalTime.now().hour),
) {
    val sky = skyPalette(time)
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    val scroll = rememberScrollState()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sceneHeight = if (maxHeight < 640.dp) 250.dp else 320.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(scroll),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(insets.calculateTopPadding() + sceneHeight),
            ) {
                SkyScene(
                    time = time,
                    parallax = { scroll.value.toFloat() },
                    fadeInto = MaterialTheme.colorScheme.background,
                    modifier = Modifier.matchParentSize(),
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = insets.calculateTopPadding() + 36.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.displayMedium,
                        color = sky.onSky,
                        modifier = Modifier.semantics { heading() },
                    )
                    // A wash of sky behind the line keeps it readable over clouds and the sun.
                    Surface(shape = CircleShape, color = sky.skyTop.copy(alpha = 0.72f), contentColor = sky.onSky) {
                        Text(
                            text = stringResource(R.string.auth_tagline),
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .pullUp(72.dp)
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                FormCard(
                    mode = mode,
                    onModeChange = onModeChange,
                    form = form,
                    onFormChange = onFormChange,
                    errors = errors,
                    serverError = serverError,
                    submitting = submitting,
                    onSubmit = onSubmit,
                )
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.auth_privacy_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun FormCard(
    mode: AuthMode,
    onModeChange: (AuthMode) -> Unit,
    form: AuthForm,
    onFormChange: (AuthForm) -> Unit,
    errors: FormErrors,
    serverError: String?,
    submitting: Boolean,
    onSubmit: () -> Unit,
) {
    val focus = LocalFocusManager.current
    val registering = mode == AuthMode.REGISTER
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 22.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(if (registering) R.string.auth_heading_register else R.string.auth_heading_sign_in),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            SegmentedControl(
                options = AuthMode.entries,
                selected = mode,
                label = {
                    stringResource(if (it == AuthMode.SIGN_IN) R.string.auth_tab_sign_in else R.string.auth_tab_register)
                },
                onSelect = onModeChange,
                modifier = Modifier.fillMaxWidth(),
            )
            AnimatedVisibility(
                visible = registering,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Field(
                    value = form.name,
                    onValueChange = { onFormChange(form.copy(name = it)) },
                    label = stringResource(R.string.auth_name),
                    error = errors.name?.let { formErrorMessage(it) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    onImeAction = { focus.moveFocus(FocusDirection.Down) },
                )
            }
            Field(
                value = form.email,
                onValueChange = { onFormChange(form.copy(email = it)) },
                label = stringResource(R.string.auth_email),
                error = errors.email?.let { formErrorMessage(it) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                onImeAction = { focus.moveFocus(FocusDirection.Down) },
            )
            PasswordField(
                value = form.password,
                onValueChange = { onFormChange(form.copy(password = it)) },
                label = stringResource(R.string.auth_password),
                supporting = if (registering) stringResource(R.string.auth_password_hint) else null,
                error = errors.password?.let { formErrorMessage(it) },
                imeAction = if (registering) ImeAction.Next else ImeAction.Done,
                onImeAction = { if (registering) focus.moveFocus(FocusDirection.Down) else onSubmit() },
            )
            AnimatedVisibility(
                visible = registering,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                PasswordField(
                    value = form.confirm,
                    onValueChange = { onFormChange(form.copy(confirm = it)) },
                    label = stringResource(R.string.auth_password_confirm),
                    supporting = null,
                    error = errors.confirm?.let { formErrorMessage(it) },
                    imeAction = ImeAction.Done,
                    onImeAction = onSubmit,
                )
            }
            AnimatedVisibility(visible = serverError != null) {
                ErrorBanner(serverError.orEmpty())
            }
            Button(
                onClick = {
                    focus.clearFocus()
                    onSubmit()
                },
                enabled = !submitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) {
                if (submitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.auth_working))
                } else {
                    Text(
                        stringResource(if (registering) R.string.auth_submit_register else R.string.auth_submit_sign_in),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    keyboardOptions: KeyboardOptions,
    onImeAction: () -> Unit,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    supporting: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = error != null,
        supportingText = (error ?: supporting)?.let { text -> { Text(text) } },
        keyboardOptions = keyboardOptions,
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        visualTransformation = visualTransformation,
        trailingIcon = trailing,
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    supporting: String?,
    error: String?,
    imeAction: ImeAction,
    onImeAction: () -> Unit,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    val toggleDescription = stringResource(
        if (visible) R.string.auth_hide_password_description else R.string.auth_show_password_description,
    )
    Field(
        value = value,
        onValueChange = onValueChange,
        label = label,
        error = error,
        supporting = supporting,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        onImeAction = onImeAction,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailing = {
            TextButton(
                onClick = { visible = !visible },
                modifier = Modifier.semantics { contentDescription = toggleDescription },
            ) {
                Text(stringResource(if (visible) R.string.auth_hide_password else R.string.auth_show_password))
            }
        },
    )
}

@Composable
private fun ErrorBanner(message: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Assertive },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
