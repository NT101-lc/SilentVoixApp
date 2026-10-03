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
import com.silentvoix.app.data.progress.Milestone
import com.silentvoix.app.recognition.HandPose
import com.silentvoix.app.recognition.Recognition
import com.silentvoix.app.recognition.TranslateSession
import com.silentvoix.app.speech.SpeechStatus
import com.silentvoix.app.ui.AppDestination
import com.silentvoix.app.ui.AppShell
import com.silentvoix.app.ui.destinationsFor
import com.silentvoix.app.data.admin.AdminOverview
import com.silentvoix.app.data.admin.FeedbackItem
import com.silentvoix.app.data.admin.FeedbackKind
import com.silentvoix.app.data.admin.ManagedUser
import com.silentvoix.app.data.api.ApiError
import com.silentvoix.app.data.auth.Account
import com.silentvoix.app.data.auth.FormError
import com.silentvoix.app.data.auth.FormErrors
import com.silentvoix.app.data.auth.UserRole
import com.silentvoix.app.ui.admin.AdminContent
import com.silentvoix.app.ui.admin.AdminSection
import com.silentvoix.app.ui.admin.Loadable
import com.silentvoix.app.ui.auth.AuthContent
import com.silentvoix.app.ui.auth.AuthForm
import com.silentvoix.app.ui.auth.AuthMode
import com.silentvoix.app.ui.scene.TimeOfDay
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

    private val adminAccount = Account("a-1", "admin@silentvoix.local", "Quản trị viên", UserRole.ADMIN)
    private val userAccount = Account("u-1", "lan@example.com", "Nguyễn Thị Lan", UserRole.USER)

    @Composable
    private fun Settings(theme: ThemeMode, account: Account = userAccount) = SettingsScreen(
        settings = AppSettings(themeMode = theme), onSettingsChange = {}, account = account, onSignOut = {},
        sendingFeedback = false, feedbackSentCount = 0, onSendFeedback = { _, _ -> },
        speechStatus = SpeechStatus.Ready, onPreviewSpeech = {}, contentPadding = PaddingValues(),
    )

    @Composable
    private fun Home(
        dark: Boolean = false,
        entries: List<HistoryEntry> = busyWeek,
        padding: PaddingValues = PaddingValues(),
        hour: Int = 16,
        celebration: Milestone? = null,
    ) = HomeContent(
        entries = entries, nowMillis = at(hour), zone = ZONE, isDarkTheme = dark, onToggleTheme = {},
        onStartTranslate = {}, onOpenSpeak = {}, onOpenHistory = {}, onSpeak = {}, contentPadding = padding,
        celebration = celebration,
    )

    @Composable
    private fun Speak(category: PhraseCategory, draft: String = "", saved: List<String> = emptyList()) = SpeakContent(
        draft = draft, onDraftChange = {}, category = category, onCategoryChange = {}, saved = saved,
        onSpeak = {}, onSave = {}, onRemove = {}, contentPadding = PaddingValues(),
    )

    @Test fun homeLight() = shot("home_light") { Home() }
    @Test fun homeDawn() = shot("home_dawn_light") { Home(hour = 6) }
    @Test fun homeDusk() = shot("home_dusk_light") { Home(hour = 17) }
    @Test fun homeNightLight() = shot("home_night_light") { Home(hour = 21) }
    @Test fun homeNightDark() = shot("home_night_dark", ThemeMode.DARK) { Home(dark = true, hour = 21) }
    @Test fun homeCelebration() = shot("home_celebration_light") { Home(celebration = Milestone.STREAK_3) }
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
        AppShell(destinationsFor(UserRole.USER), AppDestination.HOME, onDestinationChange = {}, snackbarHostState = SnackbarHostState()) { Home(padding = it) }
    }
    @Test fun shellDark() = shot("shell_home_dark", ThemeMode.DARK) {
        AppShell(destinationsFor(UserRole.USER), AppDestination.HOME, onDestinationChange = {}, snackbarHostState = SnackbarHostState()) {
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
    @Test @Config(qualifiers = "w411dp-h1500dp-xxhdpi")
    fun settingsAdminFull() = shot("settings_admin_full_light") { Settings(ThemeMode.LIGHT, adminAccount) }

    // ---- sign-in -----------------------------------------------------------------------------

    @Composable
    private fun Auth(mode: AuthMode, time: TimeOfDay = TimeOfDay.DAY, error: String? = null, form: AuthForm = AuthForm()) =
        AuthContent(
            mode = mode, onModeChange = {}, form = form, onFormChange = {}, errors = FormErrors(), serverError = error,
            submitting = false, onSubmit = {}, time = time,
        )

    @Test fun authSignInLight() = shot("auth_sign_in_light") { Auth(AuthMode.SIGN_IN, form = AuthForm(email = "lan@example.com", password = "secret12")) }
    @Test fun authSignInNightDark() = shot("auth_sign_in_night_dark", ThemeMode.DARK) { Auth(AuthMode.SIGN_IN, TimeOfDay.NIGHT) }
    @Test fun authRegisterLight() = shot("auth_register_light") { Auth(AuthMode.REGISTER, TimeOfDay.DUSK) }
    @Test fun authErrorLight() = shot("auth_error_light") {
        AuthContent(
            mode = AuthMode.SIGN_IN, onModeChange = {}, form = AuthForm(email = "lan@"), onFormChange = {},
            errors = FormErrors(email = FormError.EMAIL_INVALID, password = FormError.PASSWORD_MISSING),
            serverError = "Email hoặc mật khẩu chưa đúng.", submitting = false, onSubmit = {}, time = TimeOfDay.DAWN,
        )
    }
    @Test @Config(qualifiers = "w1280dp-h800dp-xhdpi")
    fun authTablet() = shot("auth_tablet_light") { Auth(AuthMode.SIGN_IN) }

    // ---- admin -------------------------------------------------------------------------------

    private val managed = listOf(
        ManagedUser("a-1", "admin@silentvoix.local", "Quản trị viên", UserRole.ADMIN, false, at(9) - 86_400_000L * 3, at(15)),
        ManagedUser("u-1", "lan@example.com", "Nguyễn Thị Lan", UserRole.USER, false, at(9) - 86_400_000L * 2, at(14)),
        ManagedUser("u-2", "minh.dang@example.com", "Đặng Minh", UserRole.ADMIN, false, at(9) - 86_400_000L, at(9) - 86_400_000L),
        ManagedUser("u-3", "spam@example.com", null, UserRole.USER, true, at(9) - 86_400_000L * 6, at(9) - 86_400_000L * 5),
        ManagedUser("u-4", "user@silentvoix.local", "Người dùng thử", UserRole.USER, false, at(9), null),
    )
    private val inbox = listOf(
        FeedbackItem("f-1", FeedbackKind.WRONG_RESULT, "Tôi làm ký hiệu \"Đồng ý\" nhưng app nhận thành \"Xin chào\" khi trời tối.", at(15) - 600_000, null, "lan@example.com", "Nguyễn Thị Lan"),
        FeedbackItem("f-2", FeedbackKind.IDEA, "Mong có thêm các câu dùng ở bệnh viện, như \"Tôi bị dị ứng thuốc\".", at(11), null, "minh.dang@example.com", "Đặng Minh"),
        FeedbackItem("f-3", FeedbackKind.BUG, "Ứng dụng tắt khi xoay ngang màn hình lúc đang dịch.", at(9) - 86_400_000L, null, null, null),
    )

    @Composable
    private fun Admin(
        section: AdminSection,
        overview: Loadable<AdminOverview> = Loadable.Loaded(AdminOverview(128, 3, 2, 17, 64, 3, 21)),
        feedback: Loadable<List<FeedbackItem>> = Loadable.Loaded(inbox),
        query: String = "",
    ) = AdminContent(
        section = section, onSectionChange = {}, overview = overview, users = Loadable.Loaded(managed),
        feedback = feedback, query = query, onQueryChange = {}, feedbackOpenOnly = true, onFeedbackOpenOnlyChange = {},
        currentUserId = "a-1", busyIds = emptySet(), backendStatus = online, onRetryBackend = {}, onRefresh = {},
        onUserAction = { _, _ -> }, onToggleResolved = {}, nowMillis = at(16), zone = ZONE, contentPadding = PaddingValues(),
    )

    @Test fun adminOverviewLight() = shot("admin_overview_light") { Admin(AdminSection.OVERVIEW) }
    @Test fun adminOverviewDark() = shot("admin_overview_dark", ThemeMode.DARK) { Admin(AdminSection.OVERVIEW) }
    @Test @Config(qualifiers = "w411dp-h1400dp-xxhdpi")
    fun adminUsersLight() = shot("admin_users_light") { Admin(AdminSection.USERS) }
    @Test fun adminUsersSearchDark() = shot("admin_users_search_dark", ThemeMode.DARK) { Admin(AdminSection.USERS, query = "dang") }
    @Test @Config(qualifiers = "w411dp-h1200dp-xxhdpi")
    fun adminFeedbackLight() = shot("admin_feedback_light") { Admin(AdminSection.FEEDBACK) }
    @Test fun adminFeedbackEmpty() = shot("admin_feedback_empty_light") { Admin(AdminSection.FEEDBACK, feedback = Loadable.Loaded(emptyList())) }
    @Test fun adminOffline() = shot("admin_offline_light") {
        Admin(AdminSection.OVERVIEW, overview = Loadable.Failed(ApiError.NETWORK))
    }
    @Test fun shellAdmin() = shot("shell_admin_light") {
        AppShell(destinationsFor(UserRole.ADMIN), AppDestination.ADMIN, onDestinationChange = {}, snackbarHostState = SnackbarHostState()) {
            Admin(AdminSection.OVERVIEW)
        }
    }
    @Test fun settingsDark() = shot("settings_dark", ThemeMode.DARK) { Settings(ThemeMode.DARK) }
}

/** Stands in for the camera image: a dim, lamp-lit room so captions are judged over "video". */
@Composable
private fun CameraStandIn(modifier: Modifier) {
    Box(
        modifier.background(
            Brush.radialGradient(listOf(Color(0xFF7C8A86), Color(0xFF283240)), radius = 1100f),
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

/** NOW's day at [hour]:45, to paint the home scene at other times of day. */
private fun at(hour: Int): Long = LocalDateTime.of(2026, 9, 29, hour, 45).atZone(ZONE).toInstant().toEpochMilli()

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
