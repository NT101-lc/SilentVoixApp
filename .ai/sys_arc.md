<!-- Purpose: repository map, runtime flow, and ownership boundaries. Updated: 2026-11-25 +07:00 -->
# System Architecture
- `android/app/src/main/java/com/silentvoix/app/`: Kotlin, Compose, Material 3; `ui/` (composables + ViewModels), `domain/` (models, rules, repository interfaces), `data/` (implementations), `recognition/`, `speech/`.
- `backend-java/src/main/java/com/silentvoix/backend/`: Java 21, Spring Boot 4; `auth/`, `admin/`, `health/`, `feedback/`, `database/`.
- Layers (both projects): user -> logic -> repository; logic depends on repository interfaces only. See `CLAUDE.md` "Architecture".
- Android flow: Composable `*Screen` -> `*ViewModel` -> `domain` repository interface -> `data/` implementation (`SilentVoixApi` -> REST backend, DataStore, Room). Wired by `AppContainer` (`SilentVoixApplication`).
- Device-only settings/phrases: Preferences DataStore; device-only recognition history: Room.
- Backend flow: interceptor/role checks -> `*Controller` -> `*Service` (`Transactions`) -> `*Repository` interface -> `Jdbc*Repository` -> PostgreSQL; Flyway runs at startup.
- Backend owns accounts, roles, feedback, and server data. Android owns camera, gesture UI, speech, and local history.
- Android tests: `android/app/src/test`; backend tests: `backend-java/src/test`; CI runs each project separately.
- Do not add a direct Android database connection or assume planned WebSocket support is implemented.
