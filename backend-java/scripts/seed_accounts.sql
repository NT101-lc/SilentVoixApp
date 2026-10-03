-- Creates (or repairs) two accounts for development and demos:
--   admin@silentvoix.local  role admin
--   user@silentvoix.local   role user
--
-- BEFORE RUNNING: replace <admin password> and <user password> below with the passwords to sign in
-- with (8+ characters, at most 72 bytes). The script stops, changing nothing, while either is left
-- as is. Do not commit the filled-in copy.
--
-- Each account gets a verified e-mail identity, that password (stored as a BCrypt hash made by
-- pgcrypto, the same format the backend writes) and default settings. Safe to run again: an
-- account whose e-mail already exists is reused, its role is set and its password replaced.
-- Run it on a migrated database (the backend migrates on start, version 4 or later), e.g. paste it
-- into Neon's SQL Editor, or:
--   psql "postgresql://ROLE:PASSWORD@HOST/DB?sslmode=require" -f seed_accounts.filled.sql
-- It is not a Flyway migration on purpose: production never gets these accounts by itself.

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TEMPORARY TABLE seed_account (email text, role text, display_name text, password text) ON COMMIT DROP;
INSERT INTO seed_account
VALUES ('admin@silentvoix.local', 'admin', 'Quản trị viên', '<admin password>'),
       ('user@silentvoix.local', 'user', 'Người dùng thử', '<user password>');

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM seed_account WHERE password LIKE '<%password>') THEN
        RAISE EXCEPTION 'Fill in both passwords (replace <admin password> and <user password>) before running';
    END IF;
    IF EXISTS (SELECT 1 FROM seed_account WHERE char_length(password) < 8 OR octet_length(password) > 72) THEN
        RAISE EXCEPTION 'Each password needs at least 8 characters and at most 72 bytes';
    END IF;
END
$$;

-- New users only for addresses no account has yet.
WITH missing AS (
    SELECT s.*
    FROM seed_account s
    WHERE NOT EXISTS (SELECT 1 FROM user_identity i WHERE i.provider = 'email' AND i.subject = s.email)
),
created AS (
    INSERT INTO app_user (display_name, role)
    SELECT display_name, role FROM missing
    RETURNING id, display_name
)
INSERT INTO user_identity (user_id, provider, subject, verified_at)
SELECT c.id, 'email', m.email, now()
FROM created c
JOIN missing m ON m.display_name = c.display_name;

-- Existing accounts: set the role, unlock, and mark the address verified.
UPDATE app_user u
SET role = s.role, disabled_at = NULL
FROM seed_account s
JOIN user_identity i ON i.provider = 'email' AND i.subject = s.email
WHERE u.id = i.user_id AND (u.role <> s.role OR u.disabled_at IS NOT NULL);

UPDATE user_identity i
SET verified_at = now()
FROM seed_account s
WHERE i.provider = 'email' AND i.subject = s.email AND i.verified_at IS NULL;

INSERT INTO password_credential (user_id, password_hash)
SELECT i.user_id, crypt(s.password, gen_salt('bf', 10))
FROM seed_account s
JOIN user_identity i ON i.provider = 'email' AND i.subject = s.email
ON CONFLICT (user_id) DO UPDATE SET password_hash = EXCLUDED.password_hash;

INSERT INTO user_settings (user_id)
SELECT i.user_id
FROM seed_account s
JOIN user_identity i ON i.provider = 'email' AND i.subject = s.email
ON CONFLICT (user_id) DO NOTHING;

COMMIT;
