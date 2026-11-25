<!-- Purpose: dated, compact task state for continuity across sessions. Updated: 2026-11-25 +07:00 -->
# Session Memory
- Add one entry per task; keep decisions and unresolved work, not chat transcripts.
- Never store secrets, tokens, personal data, or temporary credentials.

## 2026-10-04 22:41 +07:00
- Goal: initialize `.ai/` project context files.
- State: six context files committed as `0ef16d9` and pushed; `main` synced with `origin/main` at task start.
- Decisions: derive architecture and checks from `CLAUDE.md`, source, and tests.
- Checks: staged whitespace check passed for initial files.
- Open: none.

## 2026-10-04 22:49 +07:00
- Goal: preserve user-added rule requiring a memory update when finishing file changes.
- Files: `.ai/rules.md`, `.ai/memories.md`.
- Decision: commit and push the isolated updates on `main` after validation.

## 2026-11-25
- Goal: refactor both projects into user -> logic -> repository layers following SOLID.
- Branch: `refactor/layered-architecture` (pushed; not merged, no PR).
- Backend: controllers map HTTP only; `*Service` holds rules; SQL in `*Repository` interfaces with `Jdbc*Repository` implementations; `Transactions` interface implemented by `Jdbc` (thread-bound connection). HTTP API unchanged.
- Android: `domain/` (models, rules, repository interfaces), `data/` (implementations), ViewModels per screen, `AppContainer` DI; added `lifecycle-viewmodel-compose` and `kotlinx-coroutines-test`.
- Checks: backend `mvnw verify` passed locally (126 tests, Docker). No Android SDK on this machine; Android verified only via CI `workflow_dispatch` on the branch.
- Open: `TranslateScreen` still holds its camera/session state in the composable (`TranslateSession` is the logic); merge the branch after review.
