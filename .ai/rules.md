<!-- Purpose: agent behavior, coding constraints, and scoring. Updated: 2026-10-04 22:49:46 +07:00 -->
# Rules
- Read `CLAUDE.md` and nearby code before changing behavior; treat code/tests as truth.
- Make the smallest complete change; preserve public APIs, project boundaries, and user edits.
- Android: follow Kotlin/Compose patterns; keep user-facing strings in Vietnamese resources.
- Backend: follow Java/Spring conventions; add schema changes as new Flyway migrations.
- Android uses backend REST only; never connect the app directly to PostgreSQL.
- Never expose secrets, tokens, or database URLs in logs, responses, tests, or commits.
- Run the narrowest relevant check; state what ran and what did not.
- Do not commit or push unless requested; never rewrite history or discard user changes.
- Always update memories.md when finished changing files or add new features
- Score: +1 focused implementation, +1 passing relevant test, +1 explicit caveat; -2 unrelated change, -3 secret exposure, -2 unsupported claim.
