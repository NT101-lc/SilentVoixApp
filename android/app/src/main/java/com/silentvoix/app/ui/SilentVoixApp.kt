package com.silentvoix.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.silentvoix.app.R
import com.silentvoix.app.domain.settings.ThemeMode
import com.silentvoix.app.recognition.Recognition
import com.silentvoix.app.speech.SpeakOutcome
import com.silentvoix.app.speech.SpeechUnavailableReason
import com.silentvoix.app.speech.rememberSpeech
import com.silentvoix.app.ui.admin.AdminScreen
import com.silentvoix.app.ui.admin.AdminViewModel
import com.silentvoix.app.ui.auth.AuthScreen
import com.silentvoix.app.ui.auth.AuthViewModel
import com.silentvoix.app.ui.common.apiErrorRes
import com.silentvoix.app.ui.common.rememberHapticTap
import com.silentvoix.app.ui.history.HistoryScreen
import com.silentvoix.app.ui.history.HistoryViewModel
import com.silentvoix.app.ui.home.HomeScreen
import com.silentvoix.app.ui.home.HomeViewModel
import com.silentvoix.app.ui.scene.TimeOfDay
import com.silentvoix.app.ui.scene.paperGrain
import com.silentvoix.app.ui.scene.skyPalette
import com.silentvoix.app.ui.settings.SettingsScreen
import com.silentvoix.app.ui.speak.SpeakScreen
import com.silentvoix.app.ui.speak.SpeakViewModel
import com.silentvoix.app.ui.theme.SilentVoixTheme
import com.silentvoix.app.ui.translate.TranslateScreen
import java.time.LocalTime
import kotlinx.coroutines.launch

/**
 * The app (user layer): speech, navigation and the destinations. State and decisions live in
 * [AppViewModel] and each screen's ViewModel; storage and the backend sit behind the domain's
 * repository interfaces. Nobody signed in sees the sign-in screen; a user gets five tabs, an
 * admin also gets Quản trị. It opens on the home dashboard; the camera is only ever started from
 * the Translate screen, or by the home screen's explicit action.
 */
@Composable
fun SilentVoixApp() {
    val app: AppViewModel = rememberViewModel { AppViewModel(it) }
    // Null until the stored settings are read (a few ms); drawing nothing meanwhile avoids a
    // flash of the default theme before a saved dark theme applies.
    val storedSettings by app.settings.collectAsStateWithLifecycle()
    val settings = storedSettings ?: return
    // Same for the session: "not read yet" must not flash the sign-in screen.
    val stored by app.session.collectAsStateWithLifecycle()
    val loadedSession = stored ?: return
    val sessionExpired by app.sessionExpired.collectAsStateWithLifecycle()
    val backendStatus by app.backendStatus.collectAsStateWithLifecycle()
    val feedbackForm by app.feedbackForm.collectAsStateWithLifecycle()
    var destination by rememberSaveable { mutableStateOf(AppDestination.HOME) }
    val screenStateHolder = rememberSaveableStateHolder()
    // Set by the home screen's "open camera" action; Translate starts a session when it sees it.
    var translateStartRequested by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = { message ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    val context = LocalContext.current
    LaunchedEffect(app) {
        app.events.collect { event ->
            when (event) {
                AppEvent.FeedbackSent -> showMessage(context.getString(R.string.feedback_sent))
                is AppEvent.Failed -> showMessage(context.getString(apiErrorRes(event.error)))
            }
        }
    }

    // Explains why nothing was heard; silent success needs no message.
    val reportSpeakOutcome: (SpeakOutcome) -> Unit = { outcome ->
        val messageRes = when (outcome) {
            is SpeakOutcome.Unavailable -> when (outcome.reason) {
                SpeechUnavailableReason.NO_ENGINE -> R.string.speech_unavailable_no_engine
                SpeechUnavailableReason.LANGUAGE_MISSING -> R.string.speech_unavailable_language
            }
            SpeakOutcome.Failed -> R.string.speech_failed
            SpeakOutcome.Spoken, SpeakOutcome.Queued -> null
        }
        messageRes?.let { showMessage(context.getString(it)) }
    }
    val speech = rememberSpeech(onDeferredOutcome = reportSpeakOutcome)
    LaunchedEffect(settings.speechRate) { speech.setRate(settings.speechRate) }
    // userInitiated: replay and preview report problems; auto-speak stays quiet so an unavailable
    // voice does not raise a message on every recognised gesture.
    val speak: (String, Boolean) -> Unit = { text, userInitiated ->
        val outcome = speech.speak(text)
        if (userInitiated) reportSpeakOutcome(outcome)
    }
    val onNewResult: (Recognition) -> Unit = { recognition ->
        app.onRecognized(recognition.text, recognition.confidencePercent)
        if (settings.autoSpeak) speak(recognition.text, false)
    }

    val systemDark = isSystemInDarkTheme()
    val isDark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val session = loadedSession.session
    val signOut: (expired: Boolean) -> Unit = { expired -> app.signOut(expired) }
    // Whoever signs in next starts on the home screen, however the last session ended.
    LaunchedEffect(session == null) {
        if (session == null) destination = AppDestination.HOME
    }
    // Asks the server who this is on start (and when the token changes).
    LaunchedEffect(session?.token) { app.refreshAccount() }

    // The home and sign-in screens are painted right under the status bar: their icons follow the sky.
    val skyIsLight = skyPalette(TimeOfDay.from(LocalTime.now().hour)).isLight
    val shownDestination = session?.let { destination.allowedFor(it.account.role) } ?: AppDestination.HOME
    SilentVoixTheme(
        themeMode = settings.themeMode,
        lightStatusBars = if (session == null || shownDestination == AppDestination.HOME) skyIsLight else null,
    ) {
        if (session == null) {
            AuthScreen(
                viewModel = rememberViewModel { AuthViewModel(it.authRepository) },
                onSignedIn = app::onSignedIn,
                notice = if (sessionExpired) stringResource(R.string.session_expired) else null,
            )
            return@SilentVoixTheme
        }
        val role = session.account.role
        val tap = rememberHapticTap(settings.haptics)
        AppShell(
            destinations = destinationsFor(role),
            destination = shownDestination,
            onDestinationChange = {
                if (it != destination) tap()
                destination = it
            },
            snackbarHostState = snackbarHostState,
        ) { contentPadding ->
            // Tabs slide a little towards the one chosen while they cross-fade. Each keeps its saveable
            // state (history filters, a typed draft) while switching.
            AnimatedContent(
                targetState = shownDestination,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    val shift = { width: Int -> if (forward) width / 12 else -width / 12 }
                    (fadeIn(tween(260, delayMillis = 60)) + slideInHorizontally(tween(320)) { shift(it) }) togetherWith
                        (fadeOut(tween(140)) + slideOutHorizontally(tween(320)) { -shift(it) })
                },
                label = "destination",
            ) { target ->
            screenStateHolder.SaveableStateProvider(target.name) {
                when (target) {
                    AppDestination.HOME -> HomeScreen(
                        viewModel = rememberViewModel { HomeViewModel(it) },
                        isDarkTheme = isDark,
                        // Leaves "follow the system" for an explicit choice: the opposite of what is showing.
                        onToggleTheme = {
                            val next = if (isDark) ThemeMode.LIGHT else ThemeMode.DARK
                            app.updateSettings { it.copy(themeMode = next) }
                        },
                        onStartTranslate = {
                            translateStartRequested = true
                            destination = AppDestination.TRANSLATE
                        },
                        onOpenSpeak = { destination = AppDestination.SPEAK },
                        onOpenHistory = { destination = AppDestination.HISTORY },
                        hapticsEnabled = settings.haptics,
                        onSpeak = { speak(it, true) },
                        celebrated = settings.celebratedMilestones,
                        contentPadding = contentPadding,
                    )
                    AppDestination.TRANSLATE -> TranslateScreen(
                        largeResultText = settings.largeResultText,
                        autoSpeak = settings.autoSpeak,
                        hapticsEnabled = settings.haptics,
                        onToggleAutoSpeak = {
                            val enabled = !settings.autoSpeak
                            app.updateSettings { it.copy(autoSpeak = enabled) }
                        },
                        onNewResult = onNewResult,
                        onReplay = { speak(it, true) },
                        contentPadding = contentPadding,
                        startRequested = translateStartRequested,
                        onStartRequestHandled = { translateStartRequested = false },
                    )
                    AppDestination.SPEAK -> SpeakScreen(
                        viewModel = rememberViewModel { SpeakViewModel(it) },
                        onSaved = { showMessage(context.getString(R.string.speak_saved)) },
                        hapticsEnabled = settings.haptics,
                        onSpeak = { speak(it, true) },
                        contentPadding = contentPadding,
                    )
                    AppDestination.HISTORY -> HistoryScreen(
                        viewModel = rememberViewModel { HistoryViewModel(it) },
                        onCleared = { showMessage(context.getString(R.string.history_cleared)) },
                        onNavigateToTranslate = { destination = AppDestination.TRANSLATE },
                        hapticsEnabled = settings.haptics,
                        onSpeak = { speak(it, true) },
                        contentPadding = contentPadding,
                    )
                    AppDestination.ADMIN -> AdminScreen(
                        // One per session: a new sign-in never sees the last one's data or token.
                        viewModel = rememberViewModel(key = "admin-${session.token.hashCode()}") {
                            AdminViewModel(it.adminRepository, session.token)
                        },
                        currentUserId = session.account.id,
                        backendStatus = backendStatus,
                        onRetryBackend = app::checkBackend,
                        onUnauthorized = { signOut(true) },
                        // A 403 means the role was taken away: refresh the account so the tab disappears.
                        onForbidden = app::refreshAccount,
                        showMessage = showMessage,
                        hapticsEnabled = settings.haptics,
                        contentPadding = contentPadding,
                    )
                    AppDestination.SETTINGS -> SettingsScreen(
                        settings = settings,
                        onSettingsChange = { changed -> app.updateSettings { changed } },
                        account = session.account,
                        onSignOut = { signOut(false) },
                        sendingFeedback = feedbackForm.sending,
                        feedbackSentCount = feedbackForm.sentCount,
                        onSendFeedback = app::sendFeedback,
                        speechStatus = speech.status,
                        onPreviewSpeech = { speak(it, true) },
                        contentPadding = contentPadding,
                    )
                }
            }
            }
        }
    }
}

/**
 * Navigation around a screen. It adapts to the window: a bottom bar on phones and a navigation
 * rail on wider windows, via [NavigationSuiteScaffold]. There is no top app bar: each screen opens
 * with its own heading so the content can start at the very top of the window.
 */
@Composable
internal fun AppShell(
    destinations: List<AppDestination>,
    destination: AppDestination,
    onDestinationChange: (AppDestination) -> Unit,
    snackbarHostState: SnackbarHostState,
    content: @Composable (PaddingValues) -> Unit,
) {
    val layoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())
    // The navigation bar / rail already pads for the system bars on its own side.
    val contentInsets = if (layoutType == NavigationSuiteType.NavigationBar) {
        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    } else {
        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Bottom + WindowInsetsSides.End)
    }
    // Read in composable scope: the navigationSuiteItems builder below is not composable.
    val itemColors = NavigationSuiteDefaults.itemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
            selectedTextColor = MaterialTheme.colorScheme.onSurface,
            indicatorColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        navigationRailItemColors = NavigationRailItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
            selectedTextColor = MaterialTheme.colorScheme.onSurface,
            indicatorColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
    NavigationSuiteScaffold(
        layoutType = layoutType,
        containerColor = MaterialTheme.colorScheme.background,
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            navigationRailContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        navigationSuiteItems = {
            destinations.forEach { target ->
                item(
                    selected = target == destination,
                    onClick = { onDestinationChange(target) },
                    // The visible label names the destination for TalkBack.
                    icon = { Icon(destinationIcon(target), contentDescription = null) },
                    label = { Text(text = stringResource(target.labelRes), maxLines = 1) },
                    colors = itemColors,
                )
            }
        },
    ) {
        // Transparent over the navigation scaffold's page colour, with paper grain between the two.
        val grainInk = MaterialTheme.colorScheme.onBackground
        Scaffold(
            modifier = Modifier.paperGrain(ink = grainInk, alpha = if (grainInk.luminance() > 0.5f) 0.05f else 0.06f),
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = contentInsets,
            content = content,
        )
    }
}

@Composable
private fun destinationIcon(destination: AppDestination): ImageVector = when (destination) {
    AppDestination.HOME -> Icons.Filled.Home
    AppDestination.TRANSLATE -> ImageVector.vectorResource(R.drawable.ic_translate)
    AppDestination.SPEAK -> ImageVector.vectorResource(R.drawable.ic_chat)
    AppDestination.HISTORY -> ImageVector.vectorResource(R.drawable.ic_history)
    AppDestination.ADMIN -> ImageVector.vectorResource(R.drawable.ic_admin)
    AppDestination.SETTINGS -> Icons.Filled.Settings
}

