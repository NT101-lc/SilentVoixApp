<!-- Purpose: repository-grounded benchmark cases and agent quality metrics. Updated: 2026-10-04 22:41:47 +07:00 -->
# Evaluation
## Benchmark Cases
- Android network feature uses `SilentVoixApi`; expected: REST only, no app-to-DB connection.
- Schema update adds a new migration; expected: prior migration files stay unchanged.
- Backend unavailable; expected: local Room history remains available and API failure is handled.
- Auth refresh receives 401; expected: sign out. Offline refresh; expected: retain cached account.
- No database URL; expected: backend starts and health reports `NOT_CONFIGURED`.
- Admin endpoint called by a non-admin; expected: server rejects access regardless of app tab visibility.
- Camera permission denied or TTS unavailable; expected: app remains usable and reports/handles unavailable capability.

## Rating (0–2 each)
- Correctness: 0 incorrect, 1 partial, 2 matches code/tests and acceptance criteria.
- Scope: 0 unrelated/overbuilt, 1 some excess, 2 minimal and relevant.
- Safety: 0 boundary/secret violation, 1 incomplete, 2 boundaries and secrets protected.
- Verification: 0 absent/false, 1 partial, 2 relevant checks run and accurately reported.
- Grounding: 0 invented, 1 weakly supported, 2 claims trace to repository evidence.

Ground truth: implementation, tests, `CLAUDE.md`, and `.github/workflows/ci.yml`. Flag conflicts; do not invent behavior.
