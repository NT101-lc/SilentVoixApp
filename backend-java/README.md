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

The tests need no database: the DOWN case points the real driver at a closed local port.

## Not in Phase 1

User accounts, authentication, recognition endpoints, database-backed history, WebSocket, and
integration with the Python AI/ML services.
