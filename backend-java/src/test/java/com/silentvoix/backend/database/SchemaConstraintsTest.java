package com.silentvoix.backend.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The schema's rules, enforced by PostgreSQL itself: each test stores something the app must never
 * store and expects the database to refuse it. Every test makes its own user, so they are independent.
 */
class SchemaConstraintsTest {

    private static final String CHECK = "23514";
    private static final String UNIQUE = "23505";
    private static final String FOREIGN_KEY = "23503";

    private static HikariDataSource db;

    @BeforeAll
    static void migrate() {
        db = TestDatabase.fresh().dataSource();
        new SchemaMigrator().migrate(db);
    }

    @AfterAll
    static void close() {
        db.close();
    }

    // ---- helpers ----------------------------------------------------------------------------

    private static int update(String sql, Object... params) throws SQLException {
        try (Connection connection = db.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            return statement.executeUpdate();
        }
    }

    private static Object scalar(String sql, Object... params) throws SQLException {
        try (Connection connection = db.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getObject(1) : null;
            }
        }
    }

    private static UUID uuid(String sql, Object... params) throws SQLException {
        return (UUID) scalar(sql, params);
    }

    private static long count(String sql, Object... params) throws SQLException {
        return ((Number) scalar(sql, params)).longValue();
    }

    private static void assertRejected(String sqlState, String sql, Object... params) {
        assertThatThrownBy(() -> update(sql, params))
                .isInstanceOf(SQLException.class)
                .extracting(e -> ((SQLException) e).getSQLState())
                .isEqualTo(sqlState);
    }

    private static UUID newUser() throws SQLException {
        return uuid("INSERT INTO app_user DEFAULT VALUES RETURNING id");
    }

    private static UUID newDevice(UUID user) throws SQLException {
        return uuid("INSERT INTO device (user_id, platform, app_version) VALUES (?, 'android', '0.1.0') RETURNING id", user);
    }

    private static UUID sign(String slug) throws SQLException {
        return uuid("SELECT id FROM sign WHERE slug = ?", slug);
    }

    private static byte[] sha256Sized(int seed) {
        byte[] bytes = new byte[32];
        bytes[0] = (byte) seed;
        return bytes;
    }

    private static Float[] landmarks(int frames) {
        Float[] values = new Float[frames * 63];
        for (int i = 0; i < values.length; i++) {
            values[i] = (i % 100) / 100f;
        }
        return values;
    }

    private static java.sql.Array realArray(Float[] values) throws SQLException {
        try (Connection connection = db.getConnection()) {
            return connection.createArrayOf("real", values);
        }
    }

    // ---- identity ---------------------------------------------------------------------------

    @Nested
    class Identity {

        @Test
        void anAnonymousUserWithADeviceAndATokenCanBeStored() throws SQLException {
            UUID device = newDevice(newUser());

            update("INSERT INTO refresh_token (device_id, token_hash, expires_at) VALUES (?, ?, now() + interval '30 days')",
                    device, sha256Sized(1));

            assertThat(count("SELECT count(*) FROM refresh_token WHERE device_id = ?", device)).isEqualTo(1);
        }

        @Test
        void anEmailIdentityMustBeLowercase() throws SQLException {
            assertRejected(CHECK, "INSERT INTO user_identity (user_id, provider, subject) VALUES (?, 'email', 'Lan@Example.com')",
                    newUser());
        }

        @Test
        void anIdentityBelongsToOneUserOnly() throws SQLException {
            update("INSERT INTO user_identity (user_id, provider, subject) VALUES (?, 'google', 'g-123')", newUser());

            assertRejected(UNIQUE, "INSERT INTO user_identity (user_id, provider, subject) VALUES (?, 'google', 'g-123')",
                    newUser());
        }

        @Test
        void anUnknownProviderIsRejected() throws SQLException {
            assertRejected(CHECK, "INSERT INTO user_identity (user_id, provider, subject) VALUES (?, 'facebook', 'f-1')",
                    newUser());
        }

        @Test
        void aRefreshTokenIsStoredOnlyAsA32ByteHash() throws SQLException {
            assertRejected(CHECK, "INSERT INTO refresh_token (device_id, token_hash, expires_at) VALUES (?, ?, now() + interval '1 day')",
                    newDevice(newUser()), "raw-token".getBytes());
        }

        @Test
        void aTokenHashIsUnique() throws SQLException {
            UUID device = newDevice(newUser());
            update("INSERT INTO refresh_token (device_id, token_hash, expires_at) VALUES (?, ?, now() + interval '1 day')",
                    device, sha256Sized(7));

            assertRejected(UNIQUE, "INSERT INTO refresh_token (device_id, token_hash, expires_at) VALUES (?, ?, now() + interval '1 day')",
                    device, sha256Sized(7));
        }

        @Test
        void anOtpAllowsAtMostFiveAttempts() throws SQLException {
            UUID otp = uuid("""
                    INSERT INTO email_otp (device_id, email, code_hash, expires_at)
                    VALUES (?, 'lan@example.com', ?, now() + interval '10 minutes') RETURNING id""",
                    newDevice(newUser()), sha256Sized(2));

            update("UPDATE email_otp SET attempts = 5 WHERE id = ?", otp);
            assertRejected(CHECK, "UPDATE email_otp SET attempts = 6 WHERE id = ?", otp);
        }
    }

    // ---- synced data ------------------------------------------------------------------------

    @Nested
    class SyncedData {

        @Test
        void speechRateStaysWithinTheAppsSliderBounds() throws SQLException {
            UUID user = newUser();
            update("INSERT INTO user_settings (user_id, speech_rate) VALUES (?, 2.0)", user);

            assertRejected(CHECK, "UPDATE user_settings SET speech_rate = 2.5 WHERE user_id = ?", user);
            assertRejected(CHECK, "UPDATE user_settings SET speech_rate = 0.25 WHERE user_id = ?", user);
        }

        @Test
        void themeModeMatchesTheAppsValues() throws SQLException {
            assertRejected(CHECK, "INSERT INTO user_settings (user_id, theme_mode) VALUES (?, 'SEPIA')", newUser());
        }

        @Test
        void confidenceIsAPercentage() throws SQLException {
            assertRejected(CHECK, """
                    INSERT INTO recognition (user_id, text, source, confidence, recognized_at)
                    VALUES (?, 'Xin chào', 'camera', 101, now())""", newUser());
        }

        @Test
        void aCameraResultNeedsAConfidenceButTypedTextDoesNot() throws SQLException {
            UUID user = newUser();

            assertRejected(CHECK, "INSERT INTO recognition (user_id, text, source, recognized_at) VALUES (?, 'Xin chào', 'camera', now())",
                    user);
            assertThat(update("INSERT INTO recognition (user_id, text, source, recognized_at) VALUES (?, 'Cho tôi hỏi', 'typed', now())",
                    user)).isEqualTo(1);
        }

        @Test
        void anUnknownSourceIsRejected() throws SQLException {
            assertRejected(CHECK, "INSERT INTO recognition (user_id, text, source, recognized_at) VALUES (?, 'Xin chào', 'voice', now())",
                    newUser());
        }

        @Test
        void recognisedTextIsOneTo200Characters() throws SQLException {
            UUID user = newUser();
            assertRejected(CHECK, "INSERT INTO recognition (user_id, text, source, recognized_at) VALUES (?, '', 'typed', now())", user);
            assertRejected(CHECK, "INSERT INTO recognition (user_id, text, source, recognized_at) VALUES (?, ?, 'typed', now())",
                    user, "a".repeat(201));
        }

        @Test
        void aChangeBumpsUpdatedAtSoOtherDevicesPickItUp() throws SQLException {
            UUID row = uuid("""
                    INSERT INTO recognition (user_id, text, source, confidence, recognized_at, updated_at)
                    VALUES (?, 'Đồng ý', 'camera', 90, now(), '2000-01-01') RETURNING id""", newUser());

            update("UPDATE recognition SET is_favourite = true WHERE id = ?", row);

            assertThat((Boolean) scalar("SELECT updated_at > now() - interval '1 minute' FROM recognition WHERE id = ?", row))
                    .isTrue();
        }

        @Test
        void aSavedPhraseIsUniquePerUserIgnoringCase() throws SQLException {
            UUID user = newUser();
            update("INSERT INTO saved_phrase (user_id, text) VALUES (?, 'Cho tôi một ly cà phê')", user);

            assertRejected(UNIQUE, "INSERT INTO saved_phrase (user_id, text) VALUES (?, 'CHO TÔI MỘT LY CÀ PHÊ')", user);
            // Another user may save the same words.
            assertThat(update("INSERT INTO saved_phrase (user_id, text) VALUES (?, 'Cho tôi một ly cà phê')", newUser()))
                    .isEqualTo(1);
        }

        @Test
        void aDeletedPhraseCanBeSavedAgain() throws SQLException {
            UUID user = newUser();
            update("INSERT INTO saved_phrase (user_id, text, deleted_at) VALUES (?, 'Tôi đến đón con', now())", user);

            assertThat(update("INSERT INTO saved_phrase (user_id, text) VALUES (?, 'Tôi đến đón con')", user)).isEqualTo(1);
        }

        @Test
        void aSavedPhraseIsStoredTrimmed() throws SQLException {
            assertRejected(CHECK, "INSERT INTO saved_phrase (user_id, text) VALUES (?, '  Xin chào ')", newUser());
        }
    }

    // ---- vocabulary, models and samples ---------------------------------------------------------

    @Nested
    class Training {

        @Test
        void aSampleHoldsExactly63ValuesPerFrame() throws SQLException {
            UUID device = newDevice(newUser());

            assertRejected(CHECK, """
                    INSERT INTO sign_sample (sign_id, device_id, consent_version, handedness, frame_count, landmarks)
                    VALUES (?, ?, '1', 'right', 2, ?)""", sign("xin_chao"), device, realArray(landmarks(1)));
            assertThat(update("""
                    INSERT INTO sign_sample (sign_id, device_id, consent_version, handedness, frame_count, landmarks)
                    VALUES (?, ?, '1', 'right', 2, ?)""", sign("xin_chao"), device, realArray(landmarks(2))))
                    .isEqualTo(1);
        }

        @Test
        void aReviewedSampleHasAReviewTimeAndAPendingOneDoesNot() throws SQLException {
            assertRejected(CHECK, """
                    INSERT INTO sign_sample (sign_id, consent_version, handedness, frame_count, landmarks, status)
                    VALUES (?, '1', 'left', 1, ?, 'accepted')""", sign("dong_y"), realArray(landmarks(1)));
        }

        @Test
        void onlyOneVersionOfAModelIsActiveAtATime() {
            assertRejected(UNIQUE, """
                    INSERT INTO model_version (name, version, asset_url, sha256, is_active)
                    VALUES ('mediapipe_gesture_recognizer', '2', 'https://example.com/m.task', ?, true)""",
                    "a".repeat(64));
        }

        @Test
        void aModelAssetIsServedOverHttpsWithASha256() {
            assertRejected(CHECK, """
                    INSERT INTO model_version (name, version, asset_url, sha256)
                    VALUES ('vsl', '1', 'http://example.com/m.task', ?)""", "a".repeat(64));
            assertRejected(CHECK, """
                    INSERT INTO model_version (name, version, asset_url, sha256)
                    VALUES ('vsl', '1', 'https://example.com/m.task', 'not-a-hash')""");
        }

        @Test
        void aModelLabelMustPointToAKnownSign() throws SQLException {
            UUID model = uuid("SELECT id FROM model_version WHERE is_active");

            assertRejected(FOREIGN_KEY, "INSERT INTO model_label (model_version_id, label, sign_id) VALUES (?, 'Wave', ?)",
                    model, UUID.randomUUID());
        }

        @Test
        void feedbackIsOneTo2000Characters() {
            assertRejected(CHECK, "INSERT INTO feedback (kind, message) VALUES ('idea', '')");
            assertRejected(CHECK, "INSERT INTO feedback (kind, message) VALUES ('praise', 'Hay quá')");
        }
    }

    // ---- deletion ---------------------------------------------------------------------------

    @Nested
    class Deletion {

        @Test
        void deletingAUserRemovesTheirDataButKeepsTheirSamplesAnonymously() throws SQLException {
            UUID user = newUser();
            UUID device = newDevice(user);
            update("INSERT INTO refresh_token (device_id, token_hash, expires_at) VALUES (?, ?, now() + interval '1 day')",
                    device, sha256Sized(9));
            update("INSERT INTO user_settings (user_id) VALUES (?)", user);
            update("INSERT INTO recognition (user_id, device_id, text, source, recognized_at) VALUES (?, ?, 'Xin chào', 'typed', now())",
                    user, device);
            update("INSERT INTO saved_phrase (user_id, text) VALUES (?, 'Tạm biệt')", user);
            update("INSERT INTO consent (user_id, kind, version) VALUES (?, 'sign_sample_contribution', '1')", user);
            UUID sample = uuid("""
                    INSERT INTO sign_sample (sign_id, user_id, device_id, consent_version, handedness, frame_count, landmarks)
                    VALUES (?, ?, ?, '1', 'right', 1, ?) RETURNING id""", sign("tuyet_voi"), user, device, realArray(landmarks(1)));

            update("DELETE FROM app_user WHERE id = ?", user);

            assertThat(count("SELECT count(*) FROM device WHERE id = ?", device)).isZero();
            assertThat(count("SELECT count(*) FROM refresh_token WHERE device_id = ?", device)).isZero();
            assertThat(count("SELECT count(*) FROM user_settings WHERE user_id = ?", user)).isZero();
            assertThat(count("SELECT count(*) FROM recognition WHERE user_id = ?", user)).isZero();
            assertThat(count("SELECT count(*) FROM saved_phrase WHERE user_id = ?", user)).isZero();
            assertThat(count("SELECT count(*) FROM consent WHERE user_id = ?", user)).isZero();
            assertThat(count("SELECT count(*) FROM sign_sample WHERE id = ? AND user_id IS NULL AND device_id IS NULL", sample))
                    .isEqualTo(1);
        }

        @Test
        void deletingADeviceKeepsTheUsersHistory() throws SQLException {
            UUID user = newUser();
            UUID device = newDevice(user);
            update("INSERT INTO recognition (user_id, device_id, text, source, recognized_at) VALUES (?, ?, 'Xin chào', 'typed', now())",
                    user, device);

            update("DELETE FROM device WHERE id = ?", device);

            assertThat(count("SELECT count(*) FROM recognition WHERE user_id = ? AND device_id IS NULL", user)).isEqualTo(1);
        }

        @Test
        void aSignWithSamplesCannotBeDeleted() throws SQLException {
            update("""
                    INSERT INTO sign_sample (sign_id, consent_version, handedness, frame_count, landmarks)
                    VALUES (?, '1', 'right', 1, ?)""", sign("dung_lai"), realArray(landmarks(1)));

            assertRejected(FOREIGN_KEY, "DELETE FROM sign WHERE slug = 'dung_lai'");
        }
    }
}
