-- Version 4: e-mail + password sign-in, locked accounts, and feedback an admin can work through.
-- E-mail OTP and Google stay planned; passwords work without an e-mail service.

-- At most one password per user, stored only as a BCrypt hash ($2a$/$2b$/$2y$, cost, 53 chars).
CREATE TABLE password_credential (
    user_id       uuid PRIMARY KEY REFERENCES app_user (id) ON DELETE CASCADE,
    password_hash text        NOT NULL CHECK (password_hash ~ '^\$2[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}$'),
    updated_at    timestamptz NOT NULL DEFAULT now()
);

CREATE TRIGGER password_credential_touch BEFORE UPDATE ON password_credential
    FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

-- Set by an admin to lock an account: it can no longer sign in and its sessions are revoked.
ALTER TABLE app_user
    ADD COLUMN disabled_at timestamptz;

-- Who sent it (kept even if the device goes; cleared if the account goes), and when an admin
-- marked it handled.
ALTER TABLE feedback
    ADD COLUMN user_id     uuid REFERENCES app_user (id) ON DELETE SET NULL,
    ADD COLUMN resolved_at timestamptz,
    ADD CONSTRAINT feedback_resolved_after_creation CHECK (resolved_at IS NULL OR resolved_at >= created_at);

CREATE INDEX feedback_user_idx ON feedback (user_id);
CREATE INDEX feedback_inbox_idx ON feedback (created_at DESC) WHERE resolved_at IS NULL;
