# SilentVoix Java Backend

Java backend for the SilentVoix native Android app (`../android/`). The app talks only to this
backend; the database is reached from here, never from the device.

- Group / base package: `com.silentvoix.backend`
- Port: `8081` (override with `PORT`)

## Tech stack

| Area | Choice |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4 (`spring-boot-starter-webmvc`), Maven |
| Database | PostgreSQL on Neon (HikariCP pool + PostgreSQL JDBC driver) |
| Passwords | BCrypt from `spring-security-crypto` (nothing else from Spring Security) |

## Phase 1 endpoints

`GET /api/v1/health` always returns 200 while the service is running. Database readiness is in the body:

```json
{
  "status": "UP",
  "service": "silentvoix-backend",
  "timestamp": "2026-09-28T04:30:00Z",
  "database": { "status": "UP", "latencyMs": 42, "error": null }
}
```

- `status`: `UP`, or `DEGRADED` when the database is down or not configured.
- `database.status`: `UP`, `DOWN` (with a short, credential-free `error`) or `NOT_CONFIGURED`.

Each call opens a pooled connection. On Neon this wakes a suspended compute, so the first call after
idle can take a few seconds.

## Accounts and roles

E-mail + password accounts with two roles. Every error body is `{"error": "<code>"}`; the app maps
codes to Vietnamese messages.

| Endpoint | Who | What |
|---|---|---|
| `POST /api/v1/auth/register` | anyone | `{email, password, displayName?, platform?, appVersion?}` → `201 {token, expiresAt, user}`. Always role `user`. Codes: `invalid_email`, `password_too_short` (< 8), `password_too_long` (> 72 bytes), `invalid_display_name`, `email_taken` (409) |
| `POST /api/v1/auth/login` | anyone | `{email, password, ...}` → `{token, expiresAt, user}`. `invalid_credentials` (401, same for an unknown address), `account_disabled` (403, only with the right password), `too_many_attempts` (429 after 5 wrong passwords in 15 min) |
| `GET /api/v1/auth/me` | signed in | `{id, email, displayName, role}`, read fresh from the database |
| `POST /api/v1/auth/logout` | signed in | Revokes this session only (204) |
| `POST /api/v1/feedback` | signed in | `{kind: wrong_result \| bug \| idea, message}` (1–2000 chars) → `201 {id}` |
| `GET /api/v1/admin/overview` | admin | Counts: users, admins, locked, new and active in 7 days, open and total feedback |
| `GET /api/v1/admin/users` | admin | Every account with role, `locked`, `createdAt`, `lastSeenAt` |
| `PATCH /api/v1/admin/users/{id}` | admin | `{role?, locked?}`. Locking revokes all their sessions. An admin cannot change their own account (`cannot_change_self`, 409) |
| `GET /api/v1/admin/feedback?status=open\|all` | admin | Newest first, with the author's e-mail and name |
| `PATCH /api/v1/admin/feedback/{id}` | admin | `{resolved: true\|false}` |

Signed-in calls send `Authorization: Bearer <token>`. A token is 32 random bytes; the database keeps
only its SHA-256 (`refresh_token`), tied to a `device` row per sign-in, valid 30 days.
`auth/AuthInterceptor` checks it against the database on every call, so a role change or a lock
applies at once: no token, a bad or expired one, or a locked account is 401; a non-admin on
`/api/v1/admin/**` is 403. Without a database every account call is 503 `database_unavailable`.

Admins are made only by another admin (`PATCH .../users/{id}`) or by `scripts/seed_accounts.sql`.

## Database configuration (Neon)

The settings are read from environment variables only. Without `SILENTVOIX_DATABASE_URL` the backend
still starts and reports `NOT_CONFIGURED`.

| Variable | Required | Meaning |
|---|---|---|
| `SILENTVOIX_DATABASE_URL` | for DB | JDBC URL, `jdbc:postgresql://HOST/DB?sslmode=require` |
| `SILENTVOIX_DATABASE_USERNAME` | for DB | Neon role |
| `SILENTVOIX_DATABASE_PASSWORD` | for DB | Neon role password |
| `SILENTVOIX_DATABASE_POOL_SIZE` | no | Max pool size, default 5 |
| `PORT` | no | HTTP port, default 8081 |

Use Neon's **pooled** endpoint (host contains `-pooler`). The backend logs a warning for a direct
Neon endpoint. Neon displays `postgresql://ROLE:PASSWORD@HOST/DB?sslmode=require`; rewrite it as
`jdbc:postgresql://HOST/DB?sslmode=require` and put role and password in the two separate variables.
A non-JDBC URL stops startup with an error that does not echo the value.

The pool keeps no idle connections for long (`minimumIdle=0`, 1-minute idle timeout), so Neon can scale
to zero.

### Local setup

Either export the variables in your shell / IDE run configuration, or:

```bash
cd backend-java
cp .env.example .env     # .env is git-ignored; fill in real values, no quotes
```

`application.properties` imports `.env` from the **working directory** (`optional:file:.env`), so run
the app from `backend-java/`. Real environment variables override values in `.env`.

## Running

The Maven wrapper (`mvnw`, Maven 3.9.16) is committed, so Maven does not need to be installed:

```bash
cd backend-java
./mvnw test              # health, migrations and schema rules (needs Docker for PostgreSQL)
./mvnw spring-boot:run
curl http://localhost:8081/api/v1/health
```

The schema tests start PostgreSQL 17 in Docker with Testcontainers, so Docker must be running; no
other database is needed. The health DOWN case points the real driver at a closed local port. CI runs
`./mvnw verify` (tests plus packaging) on JDK 21 for every pull request and push to `main`.

## Database schema

The tables (accounts, devices, synced history and phrases, the sign vocabulary, model versions,
training samples, feedback) are defined by Flyway scripts in `src/main/resources/db/migration/` and
explained in [`docs/schema.md`](docs/schema.md). They are applied when the service starts; set
`SILENTVOIX_DATABASE_MIGRATE_ON_STARTUP=false` to skip that. A failed migration is logged (without
the URL) and the service keeps running, reporting `DEGRADED`.

Two accounts for development and demos (`admin@silentvoix.local` as admin, `user@silentvoix.local`
as user) come from `scripts/seed_accounts.sql`, which you fill in with two passwords and run by
hand; see the schema doc.

`database.schemaVersion` in the health response is the applied version. `status` is `UP` only when
the database is reachable **and** at the newest bundled script.

## Deploying (Railway + Neon)

The repository ships what a container host needs: `Dockerfile` (JDK 21 build, JRE 21 runtime, non-root
user, packages without running tests), `.dockerignore` (keeps `.env` out of the image) and
`railway.json` (Dockerfile build, health check on `/api/v1/health`, redeploy only when
`backend-java/` changes).

1. **Neon**: create a project, open *Connect*, turn on *Connection pooling*, and note the host (it
   contains `-pooler`), database, role and password.
2. **Railway**: *New Project → Deploy from GitHub repo*, then in the service's *Settings*:
   - *Root Directory*: `backend-java`
   - *Config-as-code path*: `/backend-java/railway.json` (this path is not resolved against the root
     directory, so give it in full)
3. **Variables** (service → *Variables*), as in the table above:
   `SILENTVOIX_DATABASE_URL` = `jdbc:postgresql://HOST/DB?sslmode=require`,
   `SILENTVOIX_DATABASE_USERNAME`, `SILENTVOIX_DATABASE_PASSWORD`. Do not set `PORT`: Railway provides
   it and the backend listens on it.
4. **Domain**: *Settings → Networking → Generate Domain*, then check
   `curl https://YOUR-SERVICE.up.railway.app/api/v1/health` reports `"database":{"status":"UP"`.
5. **App**: build it against the deployed backend (release builds only allow HTTPS):
   `./gradlew assembleDebug -Psilentvoix.backendBaseUrl=https://YOUR-SERVICE.up.railway.app`

The health endpoint answers 200 even when the database is down or asleep, so a suspended Neon compute
does not fail a deploy; the body says `DEGRADED` instead.

To try the image locally:

```bash
docker build -t silentvoix-backend backend-java
docker run --rm -p 8081:8081 --env-file backend-java/.env silentvoix-backend
```

## Not in Phase 1

E-mail OTP and Google sign-in (tables exist), password reset, recognition endpoints, database-backed history, WebSocket, and
integration with the Python AI/ML services.
