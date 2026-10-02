package com.silentvoix.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.silentvoix.app.ui.scene.TimeOfDay
import com.silentvoix.app.ui.scene.paperGrain
import com.silentvoix.app.ui.scene.skyPalette
import java.time.LocalTime
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.silentvoix.app.BuildConfig
import com.silentvoix.app.R
import com.silentvoix.app.data.backend.BackendHealthClient
import com.silentvoix.app.SilentVoixApplication
import com.silentvoix.app.data.backend.BackendStatus
import com.silentvoix.app.recognition.Recognition
import com.silentvoix.app.speech.SpeakOutcome
import com.silentvoix.app.speech.SpeechUnavailableReason
import com.silentvoix.app.speech.rememberSpeech
import com.silentvoix.app.ui.common.rememberHapticTap
import com.silentvoix.app.ui.history.HistoryScreen
import com.silentvoix.app.ui.home.HomeScreen
import com.silentvoix.app.ui.settings.SettingsScreen
import com.silentvoix.app.ui.speak.SpeakScreen
import com.silentvoix.app.ui.theme.SilentVoixTheme
import com.silentvoix.app.ui.theme.ThemeMode
import com.silentvoix.app.ui.translate.TranslateScreen
import kotlinx.coroutines.launch

/**
 * The app: stores, speech and the five destinations. It opens on the home dashboard; the camera
 * is only ever started from the Translate screen, or by the home screen's explicit action.
 */
@Composable
fun SilentVoixApp() {
    val app = LocalContext.current.applicationContext as SilentVoixApplication
    // Null until the stored settings are read (a few ms); drawing nothing meanwhile avoids a
    // flash of the default theme before a saved dark theme applies.
    val storedSettings by app.settingsRepository.settings.collectAsStateWithLifecycle(initialValue = null)
    val settings = storedSettings ?: return
    var destination by rememberSaveable { mutableStateOf(AppDestination.HOME) }
    val screenStateHolder = rememberSaveableStateHolder()
    // Set by the home screen's "open camera" action; Translate starts a session when it sees it.
    var translateStartRequested by remember { mutableStateOf(false) }

    val healthClient = remember { BackendHealthClient(BuildConfig.BACKEND_BASE_URL) }
    var backendStatus by remember { mutableStateOf<BackendStatus>(BackendStatus.Checking) }
    var healthCheckRequest by remember { mutableIntStateOf(0) }
    LaunchedEffect(healthCheckRequest) {
        backendStatus = BackendStatus.Checking
        backendStatus = healthClient.fetchStatus()
    }
    val retryHealthCheck: () -> Unit = { healthCheckRequest++ }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = { message ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    val context = LocalContext.current
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
    // Every recognised phrase is saved to history; auto-speak stays quiet if speech is unavailable
    // so an unavailable voice does not raise a message on every gesture.
    val onNewResult: (Recognition) -> Unit = { recognition ->
        app.appScope.launch {
            app.historyRepository.add(recognition.text, recognition.confidencePercent)
        }
        if (settings.autoSpeak) speak(recognition.text, false)
    }

    val systemDark = isSystemInDarkTheme()
    val isDark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    // The home screen is painted right under the status bar: its icons follow the sky, not the theme.
    val homeSkyIsLight = skyPalette(TimeOfDay.from(LocalTime.now().hour)).isLight
    SilentVoixTheme(
        themeMode = settings.themeMode,
        lightStatusBars = if (destination == AppDestination.HOME) homeSkyIsLight else null,
    ) {
        val tap = rememberHapticTap(settings.haptics)
        AppShell(
            destination = destination,
            onDestinationChange = {
                if (it != destination) tap()
                destination = it
            },
            snackbarHostState = snackbarHostState,
        ) { contentPadding ->
            // Tabs slide a little towards the one chosen while they cross-fade. Each keeps its saveable
            // state (history filters, a typed draft) while switching.
            AnimatedContent(
                targetState = destination,
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
                        historyRepository = app.historyRepository,
                        isDarkTheme = isDark,
                        // Leaves "follow the system" for an explicit choice: the opposite of what is showing.
                        onToggleTheme = {
                            val next = if (isDark) ThemeMode.LIGHT else ThemeMode.DARK
                            app.appScope.launch { app.settingsRepository.update { it.copy(themeMode = next) } }
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
                        onCelebrated = { milestones ->
                            app.appScope.launch {
                                app.settingsRepository.update { it.copy(celebratedMilestones = milestones) }
                            }
                        },
                        contentPadding = contentPadding,
                    )
                    AppDestination.TRANSLATE -> TranslateScreen(
                        largeResultText = settings.largeResultText,
                        autoSpeak = settings.autoSpeak,
                        hapticsEnabled = settings.haptics,
                        onToggleAutoSpeak = {
                            val enabled = !settings.autoSpeak
                            app.appScope.launch { app.settingsRepository.update { it.copy(autoSpeak = enabled) } }
                        },
                        onNewResult = onNewResult,
                        onReplay = { speak(it, true) },
                        contentPadding = contentPadding,
                        startRequested = translateStartRequested,
                        onStartRequestHandled = { translateStartRequested = false },
                    )
                    AppDestination.SPEAK -> SpeakScreen(
                        phraseRepository = app.phraseRepository,
                        onSavePhrase = { phrase ->
                            app.appScope.launch { app.phraseRepository.add(phrase) }
                            showMessage(context.getString(R.string.speak_saved))
                        },
                        onRemovePhrase = { phrase -> app.appScope.launch { app.phraseRepository.remove(phrase) } },
                        hapticsEnabled = settings.haptics,
                        onSpeak = { speak(it, true) },
                        contentPadding = contentPadding,
                    )
                    AppDestination.HISTORY -> HistoryScreen(
                        historyRepository = app.historyRepository,
                        onToggleFavourite = { entry ->
                            app.appScope.launch {
                                app.historyRepository.setFavourite(entry.id, !entry.isFavourite)
                            }
                        },
                        onClearAll = {
                            app.appScope.launch { app.historyRepository.clear() }
                            showMessage(context.getString(R.string.history_cleared))
                        },
                        onNavigateToTranslate = { destination = AppDestination.TRANSLATE },
                        hapticsEnabled = settings.haptics,
                        onSpeak = { speak(it, true) },
                        contentPadding = contentPadding,
                    )
                    AppDestination.SETTINGS -> SettingsScreen(
                        settings = settings,
                        onSettingsChange = { changed ->
                            app.appScope.launch { app.settingsRepository.update { changed } }
                        },
                        backendStatus = backendStatus,
                        onRetryBackend = retryHealthCheck,
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
            AppDestination.entries.forEach { target ->
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
    AppDestination.SETTINGS -> Icons.Filled.Settings
}
