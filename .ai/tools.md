<!-- Purpose: approved commands, test runners, and external-tool boundaries. Updated: 2026-10-04 22:41:47 +07:00 -->
# Tools
- Run commands in PowerShell from the indicated project directory; use committed wrappers.
- Android tests: from `android/`, `./gradlew testDebugUnitTest` (Windows: `gradlew.bat testDebugUnitTest`). CI: `gradlew.bat testDebugUnitTest lintDebug assembleDebug`.
- Backend tests: from `backend-java/`, `./mvnw test` (Windows: `mvnw.cmd test`). CI: `mvnw.cmd -B verify`.
- Backend local run: `mvnw.cmd spring-boot:run`; health: `curl http://localhost:8081/api/v1/health`.
- Tests touching PostgreSQL require Docker; backend otherwise starts without DB configuration.
- Git inspection allowed: `git status`, `git diff`, `git log`; do not run destructive/reset commands.
- Never print `.env`, credentials, tokens, or secret-bearing environment variables.
- MCP/external tools: use only when configured and needed; do not transmit secrets or private data.
