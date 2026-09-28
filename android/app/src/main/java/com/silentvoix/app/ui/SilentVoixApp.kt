package com.silentvoix.app.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.silentvoix.app.BuildConfig
import com.silentvoix.app.R
import com.silentvoix.app.data.backend.BackendHealthClient
import com.silentvoix.app.data.backend.BackendStatus
import com.silentvoix.app.ui.history.HistoryScreen
import com.silentvoix.app.ui.settings.AppSettings
import com.silentvoix.app.ui.settings.SettingsScreen
import com.silentvoix.app.ui.theme.SilentVoixTheme
import com.silentvoix.app.ui.translate.TranslateScreen
import kotlinx.coroutines.launch

/**
 * App shell. Navigation adapts to the window: a bottom bar on phones and a navigation rail on
 * wider windows, via [NavigationSuiteScaffold].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SilentVoixApp() {
    var settings by rememberSaveable(stateSaver = AppSettings.Saver) { mutableStateOf(AppSettings()) }
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

    val layoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())
    // The navigation bar / rail already pads for the system bars on its own side.
    val contentInsets = if (layoutType == NavigationSuiteType.NavigationBar) {
        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    } else {
        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Bottom + WindowInsetsSides.End)
    }

    SilentVoixTheme(themeMode = settings.themeMode) {
        NavigationSuiteScaffold(
            layoutType = layoutType,
            navigationSuiteItems = {
                AppDestination.entries.forEach { target ->
                    item(
                        selected = target == destination,
                        onClick = { destination = target },
                        // The visible label names the destination for TalkBack.
                        icon = { Icon(destinationIcon(target), contentDescription = null) },
                        label = { Text(stringResource(target.labelRes)) },
                    )
                }
            },
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(destination.titleRes),
                                modifier = Modifier.semantics { heading() },
                            )
                        },
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = contentInsets,
            ) { contentPadding ->
                // Keeps each tab's saveable state (e.g. a running demo) while switching tabs.
                screenStateHolder.SaveableStateProvider(destination.name) {
                    when (destination) {
                        AppDestination.TRANSLATE -> TranslateScreen(
                            backendStatus = backendStatus,
                            onRetryBackend = retryHealthCheck,
                            largeResultText = settings.largeResultText,
                            onShowMessage = showMessage,
                            contentPadding = contentPadding,
                        )
                        AppDestination.HISTORY -> HistoryScreen(
                            onNavigateToTranslate = { destination = AppDestination.TRANSLATE },
                            onShowMessage = showMessage,
                            contentPadding = contentPadding,
                        )
                        AppDestination.SETTINGS -> SettingsScreen(
                            settings = settings,
                            onSettingsChange = { settings = it },
                            backendStatus = backendStatus,
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
