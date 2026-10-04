<!-- Purpose: feature scope, implementation checklist, and edge-case tracker. Updated: 2026-10-04 22:41:47 +07:00 -->
# Feature Spec
- [x] Android auth/roles, local settings/history, speech, gesture recognition, admin and feedback UI.
- [x] Backend auth/admin/feedback/health endpoints, PostgreSQL schema and startup migrations.
- [ ] For each request, name owning module, acceptance behavior, and failure states before implementation.
- [ ] Add/update focused tests; update docs when API, schema, setup, or behavior changes.
- Edge cases: offline/backend 503; 401 sign-out; admin 403 and session refresh; missing DB; invalid input; empty/loading/error states; denied camera permission; unavailable Vietnamese TTS.
- Schema: create the next `V<n>__*.sql`; never modify a migration that may have run; update schema docs/tests.
- Keep new work below as unchecked items; check off only after implementation and verification.

## Active Work
- None.
