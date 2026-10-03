# SilentVoix Android

Native Android client for SilentVoix: gesture recognition in, spoken-language text and speech out.

- Application ID / namespace: `com.silentvoix.app`
- UI language: Vietnamese (`res/values/strings.xml`)

## Tech stack

| Area | Choice | Status |
|---|---|---|
| Language / build | Kotlin (AGP 9 built-in), Gradle Kotlin DSL, version catalog | in use |
| UI | Jetpack Compose, Material 3, `material3-adaptive-navigation-suite` | in use |
| Backend calls | `HttpURLConnection` + `org.json` to `../backend-java/` (`data/api/SilentVoixApi`) | sign-in, feedback, admin, health |
| Camera / inference | CameraX, MediaPipe Tasks (Gesture Recognizer) | in use, stock gesture model |
| Speech output | Android `TextToSpeech` (Vietnamese) | in use |

## Phase 1 UI

- **Sign-in** (`ui/auth/`): the app opens on a painted sky with a paper card to sign in or create an
  account (e-mail + password; name optional). The session is kept on the device, so it opens signed
  in, offline too; on start the app asks the server who you are, and an expired session or a locked
  account returns to sign-in with a message.
- **Roles**: a **user** gets the five everyday tabs below plus sending feedback. An **admin** gets
  all of that plus a **Quản trị** tab: an overview (accounts, active and new this week, admins,
  locked, open feedback), the account list with search (Vietnamese marks optional) where an admin
  grants or removes the admin role and locks or unlocks accounts (each asked twice where it matters;
  never their own), the feedback inbox (open / all, mark handled, reopen), and server status. The
  server enforces every rule; the app only hides what a role cannot use.
- **Adaptive navigation**: `NavigationSuiteScaffold` shows a bottom bar (`Trang chủ`, `Dịch`, `Nói`,
  `Lịch sử`, `Cài đặt`) on phones and a navigation rail on larger windows. Home and Translate switch to
  two columns at ≥ 840 dp.
- **Look**: a hand-painted countryside. The home screen opens on a scene painted for the real time of
  day (dawn, day, dusk, or a starry night with fireflies), with drifting clouds, parallax hills and
  swaying grass; other screens carry a wash of sky. Lora headlines, paper grain, springy cards,
  sliding tab changes, and small leaf-burst celebrations for streaks and phrase counts. Ambient motion
  stops when the system's animations are off.
- **Trang chủ** (where the app opens): greeting, a light/dark toggle, a hero action that opens the camera,
  a shortcut to Nói, today / total / favourite counts, a 7-day activity chart with streak and most-used
  phrase, the gestures the model recognises (tap to hear), and the latest phrases. The camera never starts
  on launch.
- **Nói**: type a sentence or tap a ready-made phrase (greetings, needs, emergency) and the phone speaks
  it and shows it full-screen for the other person. Phrases you save are kept on the device.
- **Dịch**: live front-camera preview with on-device gesture recognition and the hand's skeleton drawn
  over it. Captions sit
  inside the camera stage like subtitles (latest phrase large, the two before it faded above), so the
  reader never looks away from the signer. Camera-style controls: replay, start/stop, and an auto-speak
  toggle, each with a visible text label. A full-screen view shows the phrase to the other person. Wide
  windows add a transcript of the session. Permission and failure states explain what to do.
- **Lịch sử**: every phrase recognised on Dịch, newest first, stored on the device (Room). Filters for all /
  today / favourites, replay, favourite toggle, clear-all with confirmation, and loading, empty and error
  states driven by the store.
- **Cài đặt**: the account (name, e-mail, role, sign out), theme (system/light/dark), large result
  text, speech options, a feedback form (wrong result / bug / idea), app info. Saved with DataStore,
  so they survive restarts.
- Accessibility: 48–64 dp touch targets, headings, merged TalkBack nodes, live regions for results and
  status, and text alongside every colour indicator.

### What is real

Real: accounts and roles against the backend (`/api/v1/auth`, `/api/v1/feedback`, `/api/v1/admin`),
backend health check (`GET /api/v1/health`, on the admin tab), theme switching, large
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

Settings and the session persist in Preferences DataStores (the session file is excluded from
backups and device transfers) and history in a Room database, all on the device;
history is not synced to the backend. Nothing in the app is demo data any more.

## Backend URL

`BuildConfig.BACKEND_BASE_URL` defaults to the deployed backend,
`https://silentvoixapp-production.up.railway.app`. For a backend running on your machine, override it
per build:

```bash
./gradlew assembleDebug -Psilentvoix.backendBaseUrl=http://10.0.2.2:8081   # emulator
./gradlew assembleDebug -Psilentvoix.backendBaseUrl=http://localhost:8081  # device, with adb reverse
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

Screenshot renders of every screen and state (for design review; not part of CI):

```bash
./gradlew testDebugUnitTest -Pscreenshots   # PNGs in app/build/outputs/roborazzi/
```

CI (`.github/workflows/ci.yml`) runs exactly these three tasks on every pull request and push to `main`.
The `kotlin` version in `gradle/libs.versions.toml` must match the Kotlin bundled with AGP.
