# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Scope

This repository (`~/Documents/code/silentvoix-native/`) is the only place SilentVoix work happens.
The old `~/Documents/code/SilentVoix/` repo (Vue web app, Python/FastAPI stack) is no longer
used: do not read instructions from it, copy files from it, or edit it. `.claude/settings.json`
denies file edits there.

## Layout

Two independent projects, each opened on its own (not from the repo root):

| Dir | What | Stack | Build |
|---|---|---|---|
| `android/` | Native Android app, ID/namespace `com.silentvoix.app` | Kotlin, Jetpack Compose + Material 3 | Gradle Kotlin DSL, catalog `gradle/libs.versions.toml` |
| `backend-java/` | Java backend, package `com.silentvoix.backend` | Java 21, Spring Boot 4 (webmvc), PostgreSQL on Neon | Maven |

The app talks only to the backend (REST; WebSocket planned). It must never connect to the
database directly. The backend listens on `8081` (`PORT` overrides).

Planned but not yet in any build file: Spring WebSocket. Add a dependency only when the code that uses it lands.

## Current state: Phase 1

Backend (`backend-java/`):
- `GET /api/v1/health` (`health/HealthController`): always 200; `status` is `UP`/`DEGRADED`, and
  `database.status` is `UP`/`DOWN`/`NOT_CONFIGURED`.
- `database/DatabaseConfiguration` builds a HikariCP pool only when `SILENTVOIX_DATABASE_URL` is
  set, so the app starts without a database. Spring's DataSource auto-configuration is not on the
  classpath on purpose (no `spring-boot-starter-jdbc`).
- DB settings come from env vars `SILENTVOIX_DATABASE_URL` (JDBC form, Neon pooled `-pooler` host),
  `SILENTVOIX_DATABASE_USERNAME`, `SILENTVOIX_DATABASE_PASSWORD`, optionally loaded from a
  git-ignored `backend-java/.env` (see `.env.example`). Never log or return the URL or password.
- Schema: Flyway scripts in `src/main/resources/db/migration/` (V1 all tables, V2 seed of the 7
  stock gestures and their model), documented in `backend-java/docs/schema.md`. Covers anonymous
  users with identities linked later (email OTP / Google), devices and hashed refresh tokens,
  synced settings / history (`recognition`) / `saved_phrase` with soft delete and an `updated_at`
  trigger, the sign vocabulary, model versions and labels, consented landmark-only `sign_sample`s,
  and feedback. Add changes as new `V<n>__*.sql`; never edit one that has run.
  `database/SchemaMigrator` runs Flyway from code (no Spring Flyway/JDBC auto-config);
  `SchemaMigrationRunner` applies it at startup (`silentvoix.database.migrate-on-startup`), and a
  failure is logged by kind only and the app keeps running. Health reports
  `database.schemaVersion`; `status` is `UP` only at the newest bundled script
  (`MigrationScripts.latestVersion()`). Flyway's "Database: <URL>" log line is silenced.
- Deployment: `backend-java/Dockerfile` (JDK 21 build, JRE runtime, non-root; tests are left to
  CI) and `backend-java/railway.json` (Railway: Dockerfile build, health check on
  `/api/v1/health`). Steps for Railway + Neon are in `backend-java/README.md`. `.dockerignore`
  keeps `.env` out of the image.
- Tests (JUnit 5, `spring-boot-starter-webmvc-test`; Testcontainers PostgreSQL 17 for anything
  touching the schema, so Docker must run): `SchemaMigratorTest`, `SchemaConstraintsTest` (one test
  per rule), `SchemaMigrationRunnerTest`, `MigrationScriptsTest`, `DatabaseHealthCheckerTest` (UP/DOWN/
  NOT_CONFIGURED and schema version with a hand-rolled `FakeDataSource`, no Mockito), `HealthEndpointTest` (full
  context + MockMvc per database state; the DOWN case uses the real Hikari/PostgreSQL driver
  against a closed port and asserts no credential appears in the body), and
  `DatabaseConfigurationTest` (a malformed URL is never echoed). `HealthEndpointTest` also has a
  `RealDatabase` case: startup migrates a real database, health is UP, and no log line names it.
  `./mvnw test`.

Android (`android/`):
- Vietnamese UI in `res/values/strings.xml`. `ui/SilentVoixApp.kt` holds the stores, speech and the
  five destinations (Trang chủ / Dịch / Nói / Lịch sử / Cài đặt); its stateless `AppShell` is the
  `NavigationSuiteScaffold` (bottom bar on phones, rail on larger windows). The app opens on Trang
  chủ. The camera starts only from Dịch's start button or the home screen's "Mở camera" action
  (`startRequested` on `TranslateScreen`); never on launch.
- Real: home dashboard (`ui/home/HomeScreen.kt`, stateless `HomeContent`): greeting and date, a
  light/dark toggle, the hero action, stat tiles and a 7-day bar chart from
  `data/history/HistoryStats.kt` (`historyStats`: today, total, favourites, week, streak, top
  phrase), the gesture catalogue (`recognition/GesturePhrases.kt`: `SupportedGestures`, each with
  a drawn `HandPose.glyph`), and the latest phrases. Everything is derived from the device's
  history; nothing comes from the backend.
- Real: Speak (`ui/speak/SpeakScreen.kt`): type a sentence or tap a ready-made phrase (string
  arrays `phrases_*`), the phone speaks it and shows it full-screen (`ui/common/FullscreenCaption`).
  The user's own phrases are saved by `data/phrases/PhraseRepository` in a second Preferences
  DataStore (`phrases`), newest first, single-line, capped.
- Real: backend health check (`data/backend/BackendHealthClient`, base URL from
  `BuildConfig.BACKEND_BASE_URL`, Gradle property `silentvoix.backendBaseUrl`, default
  `http://10.0.2.2:8081`). Cleartext HTTP is allowed only in debug, only to localhost/10.0.2.2.
- Real: gesture recognition in `recognition/` + `ui/translate/GestureCamera.kt`. CameraX frames go
  to MediaPipe's stock Gesture Recognizer (7 canned gestures mapped to phrases in
  `GesturePhrases.kt`; not Vietnamese Sign Language, and the UI says so). `GestureStabilizer`
  debounces per-frame output; `TranslateSession` is the screen's state machine. The model is not
  committed: the `downloadGestureModel` Gradle task fetches it (pinned URL + SHA-256) into
  generated assets, so the first build needs network.
- Real: speech in `speech/`. `AndroidSpeechEngine` wraps `TextToSpeech` (Vietnamese, `vi-VN`);
  `SpeechController` queues text until the engine is ready, clamps the rate to `SpeechRate`
  bounds and reports why speech is unavailable; `rememberSpeech()` is app-scoped in
  `SilentVoixApp` and released on dispose. Replay (Translate/History) and the Settings preview
  show a snackbar when speech is unavailable; auto-speak stays silent. The manifest's `<queries>`
  entry for `TTS_SERVICE` is required on Android 11+.
- Real: persistence. `SilentVoixApplication` (the manifest's `android:name`) holds the stores and an
  `appScope` for writes. Settings: Preferences DataStore via `data/settings/SettingsRepository`
  (bad stored values fall back to defaults). History: Room (`data/history/`), one row per
  recognised phrase, written from `SilentVoixApp.onNewResult`; device-only, not synced to the
  backend. `HistoryUiState` (loading/error/loaded) is derived from the Room flow. Room schemas
  are exported to `android/app/schemas/` and committed: bump the version and add a migration
  when the entity changes.
- UI: Be Vietnam Pro (OFL, `res/font`, licence in `assets/licenses`) for all type. Translate is
  `TranslateScreen` (stateful: session, permission, camera) over a stateless `TranslateContent`
  with a `camera` slot; live captions sit inside the camera stage, controls are a camera-style
  bar (replay / start-stop / auto-speak), and a full-screen caption view is for showing the other
  person. The stage draws the hand's skeleton over the preview: `recognition/HandPose` turns the
  model's landmarks (reported in the unrotated sensor image's frame) upright and maps them onto
  the centre-cropped, possibly mirrored preview. Palette (`ui/theme/Color.kt`): terracotta
  primary, honey secondary, sage tertiary for "ready" states, warm cream/cacao neutrals; the
  stage (`StagePalette`) is cacao with amber guides and the home hero (`HeroPalette`) is fired
  clay, both the same in light and dark. `SilentVoixTheme` eases every scheme colour when the
  theme changes; `Modifier.warmBackdrop()` is the glow behind the home screen. `HistoryContent` is the stateless History screen (day-grouped rows, clear-all behind a
  confirmation). Shared pieces:
  `ui/common/ScreenHeader`, `SegmentedControl`. Server status lives in Settings, not on Translate.
- Screenshot renders (design review, not regression tests): `app/src/test/.../screenshots/`,
  Roborazzi + Robolectric (SDK 35), excluded from normal runs and CI. Render with
  `./gradlew testDebugUnitTest -Pscreenshots`; PNGs land in `app/build/outputs/roborazzi/`.
- Nothing is demo-only any more; no demo data or `DemoBadge` remains. If a placeholder is ever
  needed again, label it visibly in the UI.
- Unit tests (JUnit 4, `app/src/test`) cover `GestureStabilizer`, `TranslateSession`, `HandPose`,
  `SpeechController`, `SettingsRepository` and `PhraseRepository` (against real DataStore files),
  `HistoryUiState`, `historyStats` and history day labels: `./gradlew testDebugUnitTest`.

Both wrappers are committed (Gradle under `android/`, Maven 3.9.16 `mvnw` under `backend-java/`);
neither `gradle` nor `mvn` needs to be on the PATH. CI is `.github/workflows/ci.yml`: on pull
requests and pushes to `main` it runs `./mvnw -B verify` (JDK 21) and
`./gradlew testDebugUnitTest lintDebug assembleDebug` (JDK 21) as two separate checks.

## Commands

```bash
# Android
cd android
./gradlew assembleDebug
./gradlew assembleDebug -Psilentvoix.backendBaseUrl=http://localhost:8081   # with adb reverse tcp:8081 tcp:8081

# Backend (run from backend-java/ so .env is picked up)
cd backend-java
./mvnw test
./mvnw spring-boot:run
curl http://localhost:8081/api/v1/health
```

## Toolchain notes

- AGP 9.x with built-in Kotlin support: the app module applies `com.android.application`, the Compose compiler plugin, KSP and the Room plugin; do not add `org.jetbrains.kotlin.android`. The catalog's `kotlin` version (Compose plugin) must match the Kotlin bundled with AGP. KSP is 2.3.x, which is decoupled from the Kotlin version.
- Core library desugaring is on so `java.time` works on `minSdk` 24.
- Icons: `material-icons-core` plus vector drawables in `res/drawable` for icons core lacks (translate, history, volume, videocam, stop). Don't add `material-icons-extended`.
- `compileSdk`/`targetSdk` 37 (the SDK platform installed locally), `minSdk` 24 (the MediaPipe Tasks minimum), Java 17 bytecode.
- The local JDK is OpenJDK 27 (via mise); the backend targets Java 21.
