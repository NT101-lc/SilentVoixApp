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
| Camera / inference | CameraX, MediaPipe Tasks (Gesture Recognizer) | in use, stock gesture model |
| Speech output | Android `TextToSpeech` (Vietnamese) | in use |

## Phase 1 UI

- **Adaptive navigation**: `NavigationSuiteScaffold` shows a bottom bar (`Dịch`, `Lịch sử`, `Cài đặt`) on
  phones and a navigation rail on larger windows. The Translate screen switches to two panes at ≥ 840 dp.
- **Dịch** (main screen): live front-camera preview with on-device gesture recognition, start/stop
  button, camera-permission and failure states, backend status, and a result card with a replay control.
- **Lịch sử**: every phrase recognised on Dịch, newest first, stored on the device (Room). Filters for all /
  today / favourites, replay, favourite toggle, and loading, empty and error states driven by the store.
- **Cài đặt**: theme (system/light/dark), large result text, speech options, app info. Saved with
  DataStore, so they survive restarts.
- Accessibility: 48–64 dp touch targets, headings, merged TalkBack nodes, live regions for results and
  status, and text alongside every colour indicator.

### What is real

Real: backend health check (`GET /api/v1/health`, including database readiness), theme switching, large
result text, gesture recognition (below), and speech: replay on Translate and History, auto-speak of
new results, and speech rate (0.5×–2×) via Android `TextToSpeech` in Vietnamese. Settings shows whether
a Vietnamese voice is available, with a preview button, or a shortcut to install the voice data.

Recognition: CameraX streams frames to MediaPipe's pretrained Gesture Recognizer, on the device. It knows
7 common hand gestures (open palm, thumb up/down, victory, pointing up, closed fist, "I love you"), which
`recognition/GesturePhrases.kt` maps to Vietnamese phrases. **It is not Vietnamese Sign Language**, and the
result card says so; a custom VSL model would replace the `.task` file and that table.
`recognition/GestureStabilizer.kt` only reports a gesture once it is held for 5 frames in a row.

The model (`gesture_recognizer.task`, ~8 MB) is not committed: the `downloadGestureModel` Gradle task
fetches a pinned version into `build/generated/models` and checks its SHA-256, so the first build needs
network access.

Settings persist in a Preferences DataStore and history in a Room database, both on the device only;
history is not synced to the backend. Nothing in the app is demo data any more.

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

## Building

Open this `android/` directory in Android Studio, or use the committed Gradle wrapper (Gradle 9.6; needs
JDK 17+ and the Android SDK with platform 37, which AGP installs if missing):

```bash
cd android
./gradlew testDebugUnitTest   # unit tests (recognition, speech, settings, history)
./gradlew lintDebug
./gradlew assembleDebug       # app/build/outputs/apk/debug/app-debug.apk
```

CI (`.github/workflows/ci.yml`) runs exactly these three tasks on every pull request and push to `main`.
The `kotlin` version in `gradle/libs.versions.toml` must match the Kotlin bundled with AGP.
