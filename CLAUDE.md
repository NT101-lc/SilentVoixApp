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

Planned but not yet in any build file: CameraX, MediaPipe Tasks, Android `TextToSpeech`, Spring
WebSocket. Add a dependency only when the code that uses it lands.

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

Android (`android/`):
- Vietnamese UI in `res/values/strings.xml`. `ui/SilentVoixApp.kt` uses `NavigationSuiteScaffold`
  (bottom bar on phones, rail on larger windows) for Dịch / Lịch sử / Cài đặt.
- Real: backend health check (`data/backend/BackendHealthClient`, base URL from
  `BuildConfig.BACKEND_BASE_URL`, Gradle property `silentvoix.backendBaseUrl`, default
  `http://10.0.2.2:8081`). Cleartext HTTP is allowed only in debug, only to localhost/10.0.2.2.
- Demo only, and must stay visibly labelled as such (`DemoBadge`) until implemented: recognition
  results (`data/demo/DemoData`), the camera preview, speech/replay, history entries and its
  empty/error states. Settings are in memory only.
- No tests exist yet.

No Gradle wrapper and no Maven wrapper are committed, and neither `gradle` nor `mvn` is on the
PATH, so nothing has been built. Generate the Gradle wrapper via Android Studio (or
`gradle wrapper`), and the Maven wrapper with `mvn wrapper:wrapper`.

## Commands (once wrappers exist)

```bash
# Android
cd android
./gradlew assembleDebug
./gradlew assembleDebug -Psilentvoix.backendBaseUrl=http://localhost:8081   # with adb reverse tcp:8081 tcp:8081

# Backend (run from backend-java/ so .env is picked up)
cd backend-java
./mvnw spring-boot:run
curl http://localhost:8081/api/v1/health
```

## Toolchain notes

- AGP 9.x with built-in Kotlin support: the app module applies `com.android.application` and the Compose compiler plugin only; do not add `org.jetbrains.kotlin.android`. The catalog's `kotlin` version (Compose plugin) must match the Kotlin bundled with AGP.
- Icons: `material-icons-core` plus vector drawables in `res/drawable` for icons core lacks (translate, history, volume, videocam, stop). Don't add `material-icons-extended`.
- `compileSdk`/`targetSdk` 37 (the SDK platform installed locally), `minSdk` 24 (the MediaPipe Tasks minimum), Java 17 bytecode.
- The local JDK is OpenJDK 27 (via mise); the backend targets Java 21.
