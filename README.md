# silentvoix-native

Native-app workspace for SilentVoix: a native Android app and a Java backend.

| Project | Purpose | Stack | Build |
|---|---|---|---|
| [`android/`](android/README.md) | Native Android client | Kotlin, Jetpack Compose + Material 3 | Gradle (Kotlin DSL) |
| [`backend-java/`](backend-java/README.md) | Backend for the native app | Java 21, Spring Boot, PostgreSQL on Neon | Maven |

## Build and test

Both projects ship their build wrapper, so neither Gradle nor Maven needs to be installed:

```bash
(cd android && ./gradlew testDebugUnitTest lintDebug assembleDebug)   # JDK 17+, Android SDK
(cd backend-java && ./mvnw verify)                                    # JDK 21+
```

GitHub Actions (`.github/workflows/ci.yml`) runs the same commands on every pull request and push to
`main`, as two checks: **Backend (Maven)** and **Android (Gradle)**.

## Architecture

The app talks to the backend over REST (HTTPS, or HTTP to localhost in debug builds). Only the backend
connects to the database.

## Status: Phase 1

- Backend: `GET /api/v1/health` with a Neon PostgreSQL readiness check, configured through environment
  variables (`backend-java/.env.example`).
- Android: adaptive Vietnamese UI (Dịch / Lịch sử / Cài đặt) with a real backend health check and on-device
  gesture recognition (CameraX + MediaPipe's stock gesture model, not yet Vietnamese Sign Language). Speech uses
  Android TextToSpeech (Vietnamese). Settings (DataStore) and recognition history
  (Room) are stored on the device.
