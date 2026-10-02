-- What a user may do. Everyone starts as 'user'; 'admin' can later review sign samples, manage the
-- vocabulary and publish model versions. Granted only by an operator (see scripts/seed_accounts.sql).
ALTER TABLE app_user
    ADD COLUMN role text NOT NULL DEFAULT 'user' CHECK (role IN ('user', 'admin'));

CREATE INDEX app_user_admin_idx ON app_user (id) WHERE role = 'admin';
