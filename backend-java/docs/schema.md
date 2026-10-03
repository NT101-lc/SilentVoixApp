# Database schema

PostgreSQL 17 on Neon. The schema is defined only by the Flyway scripts in
`src/main/resources/db/migration/`; this page explains them. `SchemaMigrator` applies the scripts
when the service starts (`silentvoix.database.migrate-on-startup`, default on). The health endpoint
reports the applied version as `database.schemaVersion`, and says `UP` only once it matches the
newest bundled script.

| Script | Contents |
|---|---|
| `V1__initial_schema.sql` | All tables, constraints, indexes and the `updated_at` trigger |
| `V2__seed_stock_gestures.sql` | The 7 stock MediaPipe gestures as signs, and that model as the active version |
| `V3__add_user_role.sql` | `app_user.role`: `user` (default) or `admin` |
| `V4__password_sign_in.sql` | `password_credential` (BCrypt hash per user), `app_user.disabled_at` (locked), `feedback.user_id` and `resolved_at` |

Add a change as a new `V<n>__<what>.sql`. Never edit a script that has run anywhere.

## Conventions

- **Ids** are `uuid` (`gen_random_uuid()`). The app may generate the id of a row it syncs, so a retried
  upload cannot create a duplicate.
- **Times** are `timestamptz`.
- **Enumerations** are `text` with a `CHECK`. Adding a value is a constraint change, not a type migration.
- **Synced rows** (`recognition`, `saved_phrase`) are soft-deleted with `deleted_at`, so other devices
  learn about the deletion. A trigger keeps `updated_at` current, and devices pull rows changed since
  their last sync.
- **Secrets** are never stored: refresh tokens and e-mail codes are kept as 32-byte SHA-256 hashes,
  passwords as BCrypt hashes (both enforced by `CHECK`).
- **No images** are stored anywhere. Training samples are hand landmarks only.

## Diagram

```mermaid
erDiagram
    app_user ||--o{ user_identity : "signs in with"
    app_user ||--o{ device : owns
    device ||--o{ refresh_token : "has sessions"
    device ||--o{ email_otp : requests
    app_user ||--o| user_settings : has
    app_user ||--o{ recognition : produced
    device |o--o{ recognition : "on"
    app_user ||--o{ saved_phrase : keeps
    app_user ||--o{ consent : gives
    sign_category |o--o{ sign : groups
    sign ||--o{ model_label : "is output as"
    model_version ||--o{ model_label : outputs
    sign |o--o{ recognition : "recognised as"
    model_version |o--o{ recognition : "by"
    sign ||--o{ sign_sample : "example of"
    app_user |o--o{ sign_sample : contributed
    device |o--o{ sign_sample : "recorded on"
    device |o--o{ feedback : sends
    app_user |o--o{ feedback : writes
    app_user ||--o| password_credential : "signs in with"
    recognition |o--o{ feedback : about
```

## Tables

### Identity

A user exists from the app's first launch, anonymously. Signing in later links an identity to that
same user, so nothing is lost.

| Table | Purpose | Key rules |
|---|---|---|
| `app_user` | One person | `display_name` optional, 1–80 chars; `role` is `user` (default) or `admin`; `disabled_at` set while an admin has locked the account |
| `password_credential` | A user's password | At most one per user; only a BCrypt hash (`CHECK` on the format) |
| `user_identity` | A verified way to sign in: `email` (OTP) or `google` | `UNIQUE (provider, subject)`; e-mail subjects are lower case |
| `device` | An installation of the app | `platform` in android / ios / web |
| `refresh_token` | Long-lived session for one device | Hash only, unique; expires after it is created |
| `email_otp` | A code e-mailed to link an address | Hash only; at most 5 attempts |

### Synced user data

These mirror what the app keeps on the device today (DataStore settings, Room history, saved phrases).

| Table | Purpose | Key rules |
|---|---|---|
| `user_settings` | 1–1 with the user | `theme_mode` in SYSTEM / LIGHT / DARK; `speech_rate` 0.5–2.0, the same bounds as the app's slider |
| `recognition` | One phrase the user produced | `source` in camera / typed / phrase; a camera result needs `confidence` 0–100; text 1–200 chars; links to `sign` and `model_version` when known |
| `saved_phrase` | The user's own phrases for the Speak screen | Trimmed, 1–200 chars; unique per user ignoring case among rows not deleted |

### Vocabulary and models

| Table | Purpose | Key rules |
|---|---|---|
| `sign_category` | Groups signs (e.g. basic gestures) | `slug` is lower-case `[a-z0-9_]` |
| `sign` | A sign and the Vietnamese it stands for | `kind` static (one hand shape) or dynamic (movement); can't be deleted while samples exist |
| `model_version` | A downloadable recognition model | HTTPS URL plus SHA-256; one active version per model name |
| `model_label` | What each output label of a model means | Must point to an existing sign |

### Training data

| Table | Purpose | Key rules |
|---|---|---|
| `consent` | The user agreed to contribute samples (and to which text version) | Revocation after granting |
| `sign_sample` | One recording: `frame_count` frames × 21 landmarks × (x, y, z) | Exactly `frame_count × 63` values; `reviewed_at` set exactly when `status` is accepted or rejected |

### Feedback

| Table | Purpose | Key rules |
|---|---|---|
| `feedback` | A wrong result, a bug or an idea, possibly anonymous | Message 1–2000 chars; `user_id` is the author (cleared if they leave); `resolved_at` is set when an admin handles it |

## Deleting data

| Deleted | Effect |
|---|---|
| A user | Their identities, devices (and so tokens and codes), settings, history, saved phrases and consents go with them. Their sign samples **stay, unlinked** (`user_id` and `device_id` become NULL): they are anonymous training data given with consent. |
| A device | Its sessions go. The user's history stays, with `device_id` cleared. |
| A sign | Refused while any sample or model label refers to it. |

## Accounts for development and demos

`backend-java/scripts/seed_accounts.sql` creates two accounts, each with a verified e-mail identity,
a password and default settings:

| E-mail | Role |
|---|---|
| `admin@silentvoix.local` | `admin` |
| `user@silentvoix.local` | `user` |

It is deliberately not a migration, so production never gets them by itself, and it holds no
passwords: replace `<admin password>` and `<user password>` in a copy (8+ characters, at most 72
bytes; the script stops, changing nothing, while a placeholder is left), then run the copy on a
database at version 4 or later: paste it into Neon's SQL Editor, or
`psql "postgresql://ROLE:PASSWORD@HOST/DB?sslmode=require" -f seed_accounts.filled.sql`. Do not
commit the filled-in copy. pgcrypto hashes the passwords in the same BCrypt format the backend
writes. Running it again sets the roles, unlocks the accounts and replaces their passwords; an
account that already has one of these addresses is reused. Registering in the app always makes a
`user`; only an admin, or this script, makes an `admin`.

## Tests

`src/test/java/com/silentvoix/backend/database/` runs against a real PostgreSQL 17 in Docker
(Testcontainers):

- `SchemaMigratorTest`: an empty database migrates to the newest script, a second run applies
  nothing, and the seed holds the 7 gestures and their active model.
- `SeedAccountsTest`: the seed script creates exactly the two accounts, their passwords pass the
  backend's own BCrypt check, it refuses unfilled placeholders and short passwords, is idempotent,
  and reuses an existing account with the same e-mail.
- `SchemaConstraintsTest`: one test per rule above, each storing something the app must never
  store and expecting PostgreSQL to refuse it.
