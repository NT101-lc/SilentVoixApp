# silentvoix-native

Native-app workspace for SilentVoix: a native Android app and a Java backend.

| Project | Purpose | Stack | Build |
|---|---|---|---|
| [`android/`](android/README.md) | Native Android client | Kotlin, Jetpack Compose + Material 3 | Gradle (Kotlin DSL) |
| [`backend-java/`](backend-java/README.md) | Backend for the native app | Java 21, Spring Boot, PostgreSQL on Neon | Maven |

The app talks to the backend over REST (HTTPS, or HTTP to localhost in debug builds). Only the backend
connects to the database.

## Status: Phase 1

- Backend: `GET /api/v1/health` with a Neon PostgreSQL readiness check, configured through environment
  variables (`backend-java/.env.example`).
- Android: adaptive Vietnamese UI (Dịch / Lịch sử / Cài đặt) with a real backend health check. Recognition,
  camera, speech and history are demo-only and labelled as such in the app.
