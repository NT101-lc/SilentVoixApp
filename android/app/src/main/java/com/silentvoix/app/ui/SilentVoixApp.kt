package com.silentvoix.app.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.Modifier
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
import com.silentvoix.app.ui.settings.SettingsScreen
import com.silentvoix.app.ui.theme.SilentVoixTheme
import com.silentvoix.app.ui.translate.TranslateScreen
import kotlinx.coroutines.launch

/**
 * App shell. Navigation adapts to the window: a bottom bar on phones and a navigation rail on
 * wider windows, via [NavigationSuiteScaffold]. There is no top app bar — each screen opens with
 * its own heading so the content can start at the very top of the window.
 */
@Composable
fun SilentVoixApp() {
    val app = LocalContext.current.applicationContext as SilentVoixApplication
    // Null until the stored settings are read (a few ms); drawing nothing meanwhile avoids a
    // flash of the default theme before a saved dark theme applies.
    val storedSettings by app.settingsRepository.settings.collectAsStateWithLifecycle(initialValue = null)
    val settings = storedSettings ?: return
    var destination by rememberSaveable { mutableStateOf(AppDestination.TRANSLATE) }
    val screenStateHolder = rememberSaveableStateHolder()

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

    val layoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())
    // The navigation bar / rail already pads for the system bars on its own side.
    val contentInsets = if (layoutType == NavigationSuiteType.NavigationBar) {
        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    } else {
        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Bottom + WindowInsetsSides.End)
    }

    SilentVoixTheme(themeMode = settings.themeMode) {
        val tap = rememberHapticTap(settings.haptics)
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
        val navSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            navigationRailContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        )
        NavigationSuiteScaffold(
            layoutType = layoutType,
            containerColor = MaterialTheme.colorScheme.background,
            navigationSuiteColors = navSuiteColors,
            navigationSuiteItems = {
                AppDestination.entries.forEach { target ->
                    item(
                        selected = target == destination,
                        onClick = {
                            if (target != destination) tap()
                            destination = target
                        },
                        // The visible label names the destination for TalkBack.
                        icon = { Icon(destinationIcon(target), contentDescription = null) },
                        label = { Text(stringResource(target.labelRes)) },
                        colors = itemColors,
                    )
                }
            },
        ) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = contentInsets,
                modifier = Modifier,
            ) { contentPadding ->
                // Keeps each tab's saveable state (e.g. history filters) while switching tabs.
                screenStateHolder.SaveableStateProvider(destination.name) {
                    when (destination) {
                        AppDestination.TRANSLATE -> TranslateScreen(
                            backendStatus = backendStatus,
                            onRetryBackend = retryHealthCheck,
                            largeResultText = settings.largeResultText,
                            hapticsEnabled = settings.haptics,
                            onNewResult = onNewResult,
                            onReplay = { speak(it, true) },
                            contentPadding = contentPadding,
                        )
                        AppDestination.HISTORY -> HistoryScreen(
                            historyRepository = app.historyRepository,
                            onToggleFavourite = { entry ->
                                app.appScope.launch {
                                    app.historyRepository.setFavourite(entry.id, !entry.isFavourite)
                                }
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

@Composable
private fun destinationIcon(destination: AppDestination): ImageVector = when (destination) {
    AppDestination.TRANSLATE -> ImageVector.vectorResource(R.drawable.ic_translate)
    AppDestination.HISTORY -> ImageVector.vectorResource(R.drawable.ic_history)
    AppDestination.SETTINGS -> Icons.Filled.Settings
}
