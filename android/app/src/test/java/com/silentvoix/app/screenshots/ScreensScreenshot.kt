package com.silentvoix.app.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.silentvoix.app.data.backend.BackendStatus
import com.silentvoix.app.data.backend.DatabaseState
import com.silentvoix.app.data.history.HistoryEntry
import com.silentvoix.app.data.history.HistoryUiState
import com.silentvoix.app.recognition.Recognition
import com.silentvoix.app.recognition.TranslateSession
import com.silentvoix.app.speech.SpeechStatus
import com.silentvoix.app.ui.history.HistoryContent
import com.silentvoix.app.ui.history.HistoryFilter
import com.silentvoix.app.ui.settings.AppSettings
import com.silentvoix.app.ui.settings.SettingsScreen
import com.silentvoix.app.ui.theme.SilentVoixTheme
import com.silentvoix.app.ui.theme.ThemeMode
import com.silentvoix.app.ui.translate.FullscreenCaption
import com.silentvoix.app.ui.translate.TranslateContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDateTime
import java.time.ZoneId

/** Renders each screen to app/build/outputs/roborazzi/ for design review. Not a regression test. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreensScreenshot {

    @get:Rule
    val compose = createComposeRule()

    private val online = BackendStatus.Online(DatabaseState.UP)

    /** One shot per test: a compose rule can only set content once. */
    private fun shot(name: String, theme: ThemeMode = ThemeMode.LIGHT, content: @Composable () -> Unit) {
        // The stage's infinite breathing animation would otherwise keep Compose from ever idling.
        compose.mainClock.autoAdvance = false
        compose.setContent {
            SilentVoixTheme(themeMode = theme) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { content() }
            }
        }
        compose.mainClock.advanceTimeBy(2_000)
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    private val hello = Recognition("Xin chào", 94)
    private val live = TranslateSession()
        .onStartRequested(hasCameraPermission = true)
        .onEngineReady()
        .onRecognized(Recognition("Cho tôi hỏi", 86))
        .onRecognized(Recognition("Đồng ý", 91))
        .onRecognized(hello)

    @Composable
    private fun Translate(session: TranslateSession, autoSpeak: Boolean = true) = TranslateContent(
        session = session, largeResultText = true, autoSpeak = autoSpeak, onToggleSession = {}, onReplay = {},
        onToggleAutoSpeak = {}, onOpenSettings = {}, contentPadding = PaddingValues(),
        camera = { CameraStandIn(it) },
    )

    @Composable
    private fun History(filter: HistoryFilter = HistoryFilter.ALL, state: HistoryUiState = sampleHistory) =
        HistoryContent(
            state = state, filter = filter, onFilterChange = {}, nowMillis = NOW, zone = ZONE,
            onToggleFavourite = {}, onReplay = {}, onRetry = {}, onNavigateToTranslate = {},
            contentPadding = PaddingValues(),
        )

    @Composable
    private fun Settings(theme: ThemeMode) = SettingsScreen(
        settings = AppSettings(themeMode = theme), onSettingsChange = {}, backendStatus = online, onRetryBackend = {},
        speechStatus = SpeechStatus.Ready, onPreviewSpeech = {}, contentPadding = PaddingValues(),
    )

    @Test fun translateIdleLight() = shot("translate_idle_light") { Translate(TranslateSession(), autoSpeak = false) }
    @Test fun translateIdleDark() = shot("translate_idle_dark", ThemeMode.DARK) { Translate(TranslateSession(), autoSpeak = false) }
    @Test fun translateLiveLight() = shot("translate_live_light") { Translate(live) }
    @Test fun translateLiveDark() = shot("translate_live_dark", ThemeMode.DARK) { Translate(live) }
    @Test fun translateListening() = shot("translate_listening_light") {
        Translate(TranslateSession().onStartRequested(hasCameraPermission = true).onEngineReady())
    }
    @Test fun translateDenied() = shot("translate_denied_light") {
        Translate(TranslateSession().onStartRequested(hasCameraPermission = false).onPermissionResult(granted = false))
    }
    @Test fun translateFullscreen() = shot("translate_fullscreen_light") { FullscreenCaption("Xin chào", onDismiss = {}) }
    @Test @Config(qualifiers = "w1280dp-h800dp-xhdpi")
    fun translateTablet() = shot("translate_tablet_light") { Translate(live) }
    @Test fun historyLight() = shot("history_light") { History() }
    @Test fun historyDark() = shot("history_dark", ThemeMode.DARK) { History() }
    @Test fun historyEmptyFavourites() = shot("history_empty_favourites_light") {
        History(HistoryFilter.FAVOURITES, HistoryUiState.Loaded(emptyList()))
    }
    @Test fun settingsLight() = shot("settings_light") { Settings(ThemeMode.LIGHT) }
    @Test fun settingsDark() = shot("settings_dark", ThemeMode.DARK) { Settings(ThemeMode.DARK) }
}

/** Stands in for the camera image: a dim, slightly lit scene so captions are judged over "video". */
@Composable
private fun CameraStandIn(modifier: Modifier) {
    Box(
        modifier.background(
            Brush.radialGradient(listOf(Color(0xFF3B4A5E), Color(0xFF151C27)), radius = 900f),
        ),
    )
}

private val ZONE: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")
private val NOW: Long = LocalDateTime.of(2026, 9, 29, 16, 45).atZone(ZONE).toInstant().toEpochMilli()
private const val HOUR = 3_600_000L

private val sampleHistory = HistoryUiState.Loaded(
    listOf(
        HistoryEntry(1, "Xin chào", 94, NOW - HOUR / 6, isFavourite = false),
        HistoryEntry(2, "Cho tôi hỏi", 88, NOW - HOUR, isFavourite = true),
        HistoryEntry(3, "Đồng ý", 91, NOW - 3 * HOUR, isFavourite = false),
        HistoryEntry(4, "Dừng lại", 83, NOW - 26 * HOUR, isFavourite = false),
        HistoryEntry(5, "Tuyệt vời", 89, NOW - 30 * HOUR, isFavourite = false),
        HistoryEntry(6, "Tôi yêu bạn", 90, NOW - 20 * 24 * HOUR, isFavourite = true),
        HistoryEntry(7, "Không đồng ý", 81, NOW - 20 * 24 * HOUR - HOUR, isFavourite = false),
    ),
)
