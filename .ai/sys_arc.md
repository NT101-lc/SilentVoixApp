<!-- Purpose: repository map, runtime flow, and ownership boundaries. Updated: 2026-10-04 22:41:47 +07:00 -->
# System Architecture
- `android/app/src/main/java/com/silentvoix/app/`: Kotlin, Compose, Material 3; `ui/`, `data/`, `recognition/`, `speech/`.
- `backend-java/src/main/java/com/silentvoix/backend/`: Java 21, Spring Boot 4; `auth/`, `admin/`, `health/`, `feedback/`, `database/`.
- Android flow: Compose screens/app state -> `data/api/SilentVoixApi` -> REST backend.
- Device-only settings/phrases: Preferences DataStore; device-only recognition history: Room.
- Backend flow: controller -> interceptor/role checks -> JDBC transaction -> PostgreSQL; Flyway runs at startup.
- Backend owns accounts, roles, feedback, and server data. Android owns camera, gesture UI, speech, and local history.
- Android tests: `android/app/src/test`; backend tests: `backend-java/src/test`; CI runs each project separately.
- Do not add a direct Android database connection or assume planned WebSocket support is implemented.
