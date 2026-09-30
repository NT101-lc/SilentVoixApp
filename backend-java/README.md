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
./mvnw test              # health endpoint + database readiness tests
./mvnw spring-boot:run
curl http://localhost:8081/api/v1/health
```

The tests need no database: the DOWN case points the real driver at a closed local port. CI runs
`./mvnw verify` (tests plus packaging) on JDK 21 for every pull request and push to `main`.

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

User accounts, authentication, recognition endpoints, database-backed history, WebSocket, and
integration with the Python AI/ML services.
