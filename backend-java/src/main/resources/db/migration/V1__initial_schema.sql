-- SilentVoix schema, version 1. PostgreSQL 17 (Neon). Documented in backend-java/docs/schema.md.
--
-- Conventions
--   * uuid primary keys (gen_random_uuid()); rows synced from the app may carry a client-made id,
--     so a retried upload cannot create a duplicate.
--   * timestamptz everywhere.
--   * Enumerations are text + CHECK, so adding a value is a constraint change, not a type migration.
--   * Synced rows are soft-deleted (deleted_at) so other devices learn about the deletion;
--     updated_at is maintained by a trigger and drives incremental sync.

CREATE FUNCTION touch_updated_at() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END;
$$;

-- ---------------------------------------------------------------------------------------------
-- Identity: a user exists from the first app launch (anonymous); identities are linked later.
-- ---------------------------------------------------------------------------------------------

CREATE TABLE app_user (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    display_name text CHECK (display_name IS NULL OR char_length(display_name) BETWEEN 1 AND 80),
    created_at   timestamptz NOT NULL DEFAULT now(),
    updated_at   timestamptz NOT NULL DEFAULT now()
);

CREATE TRIGGER app_user_touch BEFORE UPDATE ON app_user
    FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

-- A verified way to sign in. One identity belongs to exactly one user.
CREATE TABLE user_identity (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     uuid        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    provider    text        NOT NULL CHECK (provider IN ('email', 'google')),
    -- The e-mail address (lower case) or Google's stable "sub" claim.
    subject     text        NOT NULL CHECK (char_length(subject) BETWEEN 1 AND 320),
    verified_at timestamptz,
    created_at  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT user_identity_email_lower_case CHECK (provider <> 'email' OR subject = lower(subject)),
    CONSTRAINT user_identity_provider_subject_key UNIQUE (provider, subject)
);

CREATE INDEX user_identity_user_idx ON user_identity (user_id);

CREATE TABLE device (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      uuid        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    platform     text        NOT NULL CHECK (platform IN ('android', 'ios', 'web')),
    app_version  text        NOT NULL CHECK (char_length(app_version) BETWEEN 1 AND 40),
    created_at   timestamptz NOT NULL DEFAULT now(),
    last_seen_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX device_user_idx ON device (user_id);

-- Long-lived session per device. Only a SHA-256 of the token is stored, never the token.
CREATE TABLE refresh_token (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id  uuid        NOT NULL REFERENCES device (id) ON DELETE CASCADE,
    token_hash bytea       NOT NULL CHECK (octet_length(token_hash) = 32),
    created_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    CONSTRAINT refresh_token_hash_key UNIQUE (token_hash),
    CONSTRAINT refresh_token_expires_after_creation CHECK (expires_at > created_at)
);

CREATE INDEX refresh_token_device_idx ON refresh_token (device_id);

-- One-time code e-mailed to link an address to the requesting device's user. Hash only.
CREATE TABLE email_otp (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id   uuid        NOT NULL REFERENCES device (id) ON DELETE CASCADE,
    email       text        NOT NULL CHECK (char_length(email) BETWEEN 3 AND 320 AND email = lower(email)),
    code_hash   bytea       NOT NULL CHECK (octet_length(code_hash) = 32),
    attempts    smallint    NOT NULL DEFAULT 0 CHECK (attempts BETWEEN 0 AND 5),
    created_at  timestamptz NOT NULL DEFAULT now(),
    expires_at  timestamptz NOT NULL,
    consumed_at timestamptz,
    CONSTRAINT email_otp_expires_after_creation CHECK (expires_at > created_at)
);

CREATE INDEX email_otp_email_idx ON email_otp (email, created_at DESC);

-- ---------------------------------------------------------------------------------------------
-- Vocabulary and models
-- ---------------------------------------------------------------------------------------------

CREATE TABLE sign_category (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    slug       text    NOT NULL CHECK (slug ~ '^[a-z0-9_]{1,60}$'),
    name_vi    text    NOT NULL CHECK (char_length(name_vi) BETWEEN 1 AND 80),
    sort_order integer NOT NULL DEFAULT 0,
    CONSTRAINT sign_category_slug_key UNIQUE (slug)
);

-- A sign the app can recognise or collect samples for, with the Vietnamese it stands for.
CREATE TABLE sign (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id uuid        REFERENCES sign_category (id) ON DELETE SET NULL,
    slug        text        NOT NULL CHECK (slug ~ '^[a-z0-9_]{1,60}$'),
    gloss_vi    text        NOT NULL CHECK (char_length(gloss_vi) BETWEEN 1 AND 120),
    description text        CHECK (description IS NULL OR char_length(description) <= 1000),
    -- static: one hand shape; dynamic: needs movement over several frames.
    kind        text        NOT NULL DEFAULT 'static' CHECK (kind IN ('static', 'dynamic')),
    is_active   boolean     NOT NULL DEFAULT true,
    created_at  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT sign_slug_key UNIQUE (slug)
);

CREATE INDEX sign_category_idx ON sign (category_id);

-- A recognition model the app can download. One active version per model name.
CREATE TABLE model_version (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name        text        NOT NULL CHECK (name ~ '^[a-z0-9_]{1,60}$'),
    version     text        NOT NULL CHECK (char_length(version) BETWEEN 1 AND 40),
    asset_url   text        NOT NULL CHECK (asset_url LIKE 'https://%'),
    sha256      text        NOT NULL CHECK (sha256 ~ '^[0-9a-f]{64}$'),
    released_at timestamptz,
    is_active   boolean     NOT NULL DEFAULT false,
    created_at  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT model_version_name_version_key UNIQUE (name, version)
);

CREATE UNIQUE INDEX model_version_one_active_idx ON model_version (name) WHERE is_active;

-- What each output label of a model means.
CREATE TABLE model_label (
    model_version_id uuid NOT NULL REFERENCES model_version (id) ON DELETE CASCADE,
    label            text NOT NULL CHECK (char_length(label) BETWEEN 1 AND 80),
    sign_id          uuid NOT NULL REFERENCES sign (id) ON DELETE RESTRICT,
    PRIMARY KEY (model_version_id, label)
);

CREATE INDEX model_label_sign_idx ON model_label (sign_id);

-- ---------------------------------------------------------------------------------------------
-- Synced user data (mirrors the app's DataStore settings and Room history)
-- ---------------------------------------------------------------------------------------------

CREATE TABLE user_settings (
    user_id           uuid PRIMARY KEY REFERENCES app_user (id) ON DELETE CASCADE,
    theme_mode        text        NOT NULL DEFAULT 'SYSTEM' CHECK (theme_mode IN ('SYSTEM', 'LIGHT', 'DARK')),
    large_result_text boolean     NOT NULL DEFAULT true,
    auto_speak        boolean     NOT NULL DEFAULT false,
    -- Same bounds as the app's SpeechRate.
    speech_rate       real        NOT NULL DEFAULT 1.0 CHECK (speech_rate BETWEEN 0.5 AND 2.0),
    haptics           boolean     NOT NULL DEFAULT true,
    updated_at        timestamptz NOT NULL DEFAULT now()
);

CREATE TRIGGER user_settings_touch BEFORE UPDATE ON user_settings
    FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

-- One phrase the user produced: recognised from the camera, typed, or picked from a list.
CREATE TABLE recognition (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          uuid        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    device_id        uuid        REFERENCES device (id) ON DELETE SET NULL,
    text             text        NOT NULL CHECK (char_length(text) BETWEEN 1 AND 200),
    source           text        NOT NULL CHECK (source IN ('camera', 'typed', 'phrase')),
    confidence       smallint    CHECK (confidence BETWEEN 0 AND 100),
    sign_id          uuid        REFERENCES sign (id) ON DELETE SET NULL,
    model_version_id uuid        REFERENCES model_version (id) ON DELETE SET NULL,
    is_favourite     boolean     NOT NULL DEFAULT false,
    recognized_at    timestamptz NOT NULL,
    created_at       timestamptz NOT NULL DEFAULT now(),
    updated_at       timestamptz NOT NULL DEFAULT now(),
    deleted_at       timestamptz,
    CONSTRAINT recognition_camera_has_confidence CHECK (source <> 'camera' OR confidence IS NOT NULL)
);

CREATE INDEX recognition_history_idx ON recognition (user_id, recognized_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX recognition_sync_idx ON recognition (user_id, updated_at);
CREATE INDEX recognition_device_idx ON recognition (device_id);

CREATE TRIGGER recognition_touch BEFORE UPDATE ON recognition
    FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

-- The user's own phrases for the Speak screen. Single line, trimmed, unique per user ignoring case.
CREATE TABLE saved_phrase (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    uuid        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    text       text        NOT NULL CHECK (char_length(text) BETWEEN 1 AND 200 AND text = btrim(text)),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE UNIQUE INDEX saved_phrase_user_text_idx ON saved_phrase (user_id, lower(text)) WHERE deleted_at IS NULL;
CREATE INDEX saved_phrase_sync_idx ON saved_phrase (user_id, updated_at);

CREATE TRIGGER saved_phrase_touch BEFORE UPDATE ON saved_phrase
    FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

-- ---------------------------------------------------------------------------------------------
-- Training data: hand landmarks contributed with consent. Images are never stored.
-- ---------------------------------------------------------------------------------------------

CREATE TABLE consent (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    uuid        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    kind       text        NOT NULL CHECK (kind IN ('sign_sample_contribution')),
    -- The version of the consent text the user agreed to.
    version    text        NOT NULL CHECK (char_length(version) BETWEEN 1 AND 20),
    granted_at timestamptz NOT NULL DEFAULT now(),
    revoked_at timestamptz,
    CONSTRAINT consent_revoked_after_granted CHECK (revoked_at IS NULL OR revoked_at >= granted_at)
);

CREATE INDEX consent_user_idx ON consent (user_id, kind);

-- One recording of a sign: frame_count frames of 21 landmarks × (x, y, z), flattened in order.
-- Contributors are unlinked (SET NULL) when their account goes; the anonymous sample stays.
CREATE TABLE sign_sample (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    sign_id         uuid        NOT NULL REFERENCES sign (id) ON DELETE RESTRICT,
    user_id         uuid        REFERENCES app_user (id) ON DELETE SET NULL,
    device_id       uuid        REFERENCES device (id) ON DELETE SET NULL,
    consent_version text        NOT NULL CHECK (char_length(consent_version) BETWEEN 1 AND 20),
    handedness      text        NOT NULL CHECK (handedness IN ('left', 'right')),
    frame_count     smallint    NOT NULL CHECK (frame_count BETWEEN 1 AND 300),
    landmarks       real[]      NOT NULL,
    status          text        NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'accepted', 'rejected')),
    created_at      timestamptz NOT NULL DEFAULT now(),
    reviewed_at     timestamptz,
    CONSTRAINT sign_sample_landmark_count CHECK (array_ndims(landmarks) = 1 AND cardinality(landmarks) = frame_count * 63),
    CONSTRAINT sign_sample_reviewed_when_decided CHECK ((status = 'pending') = (reviewed_at IS NULL))
);

CREATE INDEX sign_sample_training_idx ON sign_sample (sign_id, status);
CREATE INDEX sign_sample_user_idx ON sign_sample (user_id);
CREATE INDEX sign_sample_device_idx ON sign_sample (device_id);

-- ---------------------------------------------------------------------------------------------
-- Feedback from the app (a wrong result, a bug, an idea). May be anonymous.
-- ---------------------------------------------------------------------------------------------

CREATE TABLE feedback (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id      uuid        REFERENCES device (id) ON DELETE SET NULL,
    recognition_id uuid        REFERENCES recognition (id) ON DELETE SET NULL,
    kind           text        NOT NULL CHECK (kind IN ('wrong_result', 'bug', 'idea')),
    message        text        NOT NULL CHECK (char_length(message) BETWEEN 1 AND 2000),
    created_at     timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX feedback_device_idx ON feedback (device_id);
CREATE INDEX feedback_recognition_idx ON feedback (recognition_id);
