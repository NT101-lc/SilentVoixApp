# SilentVoix Android

Native Android client for SilentVoix: gesture recognition in, spoken-language text and speech out.

- Application ID / namespace: `com.silentvoix.app`
- UI language: Vietnamese (`res/values/strings.xml`)

## Tech stack

| Area | Choice | Status |
|---|---|---|
| Language / build | Kotlin (AGP 9 built-in), Gradle Kotlin DSL, version catalog | in use |
| UI | Jetpack Compose, Material 3, `material3-adaptive-navigation-suite` | in use |
| Backend calls | `HttpURLConnection` + `org.json` to `../backend-java/` | health check only |
| Camera / inference | CameraX, MediaPipe Tasks | planned |
| Speech output | Android `TextToSpeech` | planned |

## Phase 1 UI

- **Adaptive navigation**: `NavigationSuiteScaffold` shows a bottom bar (`Dịch`, `Lịch sử`, `Cài đặt`) on
  phones and a navigation rail on larger windows. The Translate screen switches to two panes at ≥ 840 dp.
- **Dịch** (main screen): camera-preview placeholder, start/stop simulation button, backend and
  recognition status, and a result card with a replay control.
- **Lịch sử**: local demo entries, plus empty and error states you can preview with the chips at the top.
- **Cài đặt**: theme (system/light/dark), large result text, speech options, app info.
- Accessibility: 48–64 dp touch targets, headings, merged TalkBack nodes, live regions for results and
  status, and text alongside every colour indicator.

### Real vs demo

Real: backend health check (`GET /api/v1/health`, including database readiness), theme switching, large
result text.

Demo only (clearly labelled in the UI):
- Recognition results: canned phrases cycled by a timer. There is no camera or model.
- Camera preview: a placeholder.
- Replay / speech: shows a message; TextToSpeech is not integrated. The speech settings have no effect yet.
- History: fixed local sample entries; the empty and error states are previews.
- Settings are held in memory only and reset when the app process ends.

## Backend URL

`BuildConfig.BACKEND_BASE_URL` defaults to `http://10.0.2.2:8081` (the host machine as seen from the
emulator). Override it per build:

```bash
./gradlew assembleDebug -Psilentvoix.backendBaseUrl=http://localhost:8081
```

or set `silentvoix.backendBaseUrl=...` in `~/.gradle/gradle.properties`.

Plain HTTP is allowed only in **debug** builds and only to `10.0.2.2`, `localhost` and `127.0.0.1`
(`src/debug/res/xml/network_security_config.xml`). Release builds require HTTPS. For a physical
device, run `adb reverse tcp:8081 tcp:8081` and use `http://localhost:8081`.

## Opening

Open this `android/` directory in Android Studio. **No Gradle wrapper is committed**; let Android Studio
generate it (or run `gradle wrapper` with Gradle 9.x), then commit `gradlew`, `gradlew.bat` and
`gradle/wrapper/`.

The versions in `gradle/libs.versions.toml` (AGP 9.4.1, Kotlin 2.2.10, Compose BOM 2025.01.00,
activity-compose 1.10.1) have not been verified by a build here. If sync fails, align them first. The
`kotlin` version must match the Kotlin bundled with AGP.
