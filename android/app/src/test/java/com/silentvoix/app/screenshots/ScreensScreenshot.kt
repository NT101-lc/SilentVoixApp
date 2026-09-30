package com.silentvoix.app.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import com.silentvoix.app.recognition.SupportedGestures
import com.silentvoix.app.ui.common.HandGlyph
import com.silentvoix.app.ui.theme.StagePalette
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
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
import com.silentvoix.app.recognition.HandPose
import com.silentvoix.app.recognition.Recognition
import com.silentvoix.app.recognition.TranslateSession
import com.silentvoix.app.speech.SpeechStatus
import com.silentvoix.app.ui.AppDestination
import com.silentvoix.app.ui.AppShell
import com.silentvoix.app.ui.common.FullscreenCaption
import com.silentvoix.app.ui.history.HistoryContent
import com.silentvoix.app.ui.history.HistoryFilter
import com.silentvoix.app.ui.home.HomeContent
import com.silentvoix.app.ui.settings.AppSettings
import com.silentvoix.app.ui.settings.SettingsScreen
import com.silentvoix.app.ui.speak.PhraseCategory
import com.silentvoix.app.ui.speak.SpeakContent
import com.silentvoix.app.ui.theme.SilentVoixTheme
import com.silentvoix.app.ui.theme.ThemeMode
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
        session = session, hand = { if (session.isRunning) heldUpPalm else null }, largeResultText = true, autoSpeak = autoSpeak, onToggleSession = {}, onReplay = {},
        onToggleAutoSpeak = {}, onOpenSettings = {}, contentPadding = PaddingValues(),
        camera = { CameraStandIn(it) },
    )

    @Composable
    private fun History(filter: HistoryFilter = HistoryFilter.ALL, state: HistoryUiState = sampleHistory) =
        HistoryContent(
            state = state, filter = filter, onFilterChange = {}, nowMillis = NOW, zone = ZONE,
            onToggleFavourite = {}, onReplay = {}, onClearAll = {}, onRetry = {}, onNavigateToTranslate = {},
            contentPadding = PaddingValues(),
        )

    @Composable
    private fun Settings(theme: ThemeMode) = SettingsScreen(
        settings = AppSettings(themeMode = theme), onSettingsChange = {}, backendStatus = online, onRetryBackend = {},
        speechStatus = SpeechStatus.Ready, onPreviewSpeech = {}, contentPadding = PaddingValues(),
    )

    @Composable
    private fun Home(dark: Boolean = false, entries: List<HistoryEntry> = busyWeek, padding: PaddingValues = PaddingValues()) =
        HomeContent(
            entries = entries, nowMillis = NOW, zone = ZONE, isDarkTheme = dark, onToggleTheme = {},
            onStartTranslate = {}, onOpenSpeak = {}, onOpenHistory = {}, onSpeak = {}, contentPadding = padding,
        )

    @Composable
    private fun Speak(category: PhraseCategory, draft: String = "", saved: List<String> = emptyList()) = SpeakContent(
        draft = draft, onDraftChange = {}, category = category, onCategoryChange = {}, saved = saved,
        onSpeak = {}, onSave = {}, onRemove = {}, contentPadding = PaddingValues(),
    )

    @Test fun homeLight() = shot("home_light") { Home() }
    @Test fun homeDark() = shot("home_dark", ThemeMode.DARK) { Home(dark = true) }
    @Test fun homeNewUser() = shot("home_new_user_light") { Home(entries = emptyList()) }
    @Test @Config(qualifiers = "w411dp-h1700dp-xxhdpi")
    fun homeFullLight() = shot("home_full_light") { Home() }
    @Test @Config(qualifiers = "w411dp-h1700dp-xxhdpi")
    fun homeFullDark() = shot("home_full_dark", ThemeMode.DARK) { Home(dark = true) }
    @Test @Config(qualifiers = "w1280dp-h800dp-xhdpi")
    fun homeTablet() = shot("home_tablet_light") { Home() }
    /** The dashboard inside the real navigation, to judge the five-item bar. */
    @Test fun shellLight() = shot("shell_home_light") {
        AppShell(AppDestination.HOME, onDestinationChange = {}, snackbarHostState = SnackbarHostState()) { Home(padding = it) }
    }
    @Test fun shellDark() = shot("shell_home_dark", ThemeMode.DARK) {
        AppShell(AppDestination.HOME, onDestinationChange = {}, snackbarHostState = SnackbarHostState()) {
            Home(dark = true, padding = it)
        }
    }
    @Test fun speakLight() = shot("speak_light") { Speak(PhraseCategory.NEEDS, draft = "Cho tôi một ly cà phê sữa đá") }
    @Test fun speakEmergencyDark() = shot("speak_emergency_dark", ThemeMode.DARK) { Speak(PhraseCategory.EMERGENCY) }
    @Test fun speakMine() = shot("speak_mine_light") {
        Speak(PhraseCategory.MINE, saved = listOf("Cho tôi một ly cà phê sữa đá", "Tôi đến đón con", "Tôi đã đặt lịch hẹn lúc 9 giờ"))
    }
    @Test fun speakMineEmpty() = shot("speak_mine_empty_light") { Speak(PhraseCategory.MINE) }
    @Test fun speakFullscreen() = shot("speak_fullscreen_dark", ThemeMode.DARK) {
        FullscreenCaption("Bạn viết ra giúp tôi được không?", onDismiss = {}, onReplay = {})
    }

    /** Every gesture's hand shape, large, to judge the drawings themselves. */
    @Test fun gestureGlyphs() = shot("gesture_glyphs") {
        Column(Modifier.background(StagePalette.Ink).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SupportedGestures.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { HandGlyph(it.pose, StagePalette.Guide, StagePalette.OnInk, Modifier.size(120.dp)) }
                }
            }
        }
    }

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

/** Stands in for the camera image: a dim, warmly lit room so captions are judged over "video". */
@Composable
private fun CameraStandIn(modifier: Modifier) {
    Box(
        modifier.background(
            Brush.radialGradient(listOf(Color(0xFF8A7263), Color(0xFF3A2C25)), radius = 1100f),
        ),
    )
}

/** An open palm held in the upper middle of a portrait camera image, as the model would report it. */
private val heldUpPalm = HandPose(
    points = FloatArray(HandPose.LANDMARK_COUNT * 2) { i ->
        val p = HandPose.OpenPalm.points[i]
        if (i % 2 == 0) 0.2f + p * 0.6f else 0.12f + p * 0.45f
    },
    imageAspect = 3f / 4f,
)

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

/** A week with a gap and a clear busiest day, so the dashboard's chart and streak have something to show. */
private val busyWeek: List<HistoryEntry> = run {
    val perDay = listOf(3, 2, 5, 3, 0, 4, 2) // today first
    val texts = listOf("Xin chào", "Cho tôi hỏi", "Đồng ý", "Xin chào", "Tuyệt vời")
    var id = 1L
    perDay.flatMapIndexed { daysAgo, count ->
        List(count) { i ->
            HistoryEntry(id, texts[(id % texts.size).toInt()], 85 + i, NOW - daysAgo * 24 * HOUR - i * HOUR - HOUR / 6, isFavourite = id % 4 == 0L)
                .also { id++ }
        }
    }
}
