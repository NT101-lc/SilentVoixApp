package com.silentvoix.backend.auth;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.silentvoix.backend.api.ApiException;
import com.silentvoix.backend.database.Jdbc;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * E-mail + password accounts and per-device sessions. A session is a random token whose SHA-256
 * is a {@code refresh_token} row tied to a {@code device}; it lasts {@link #SESSION_LIFETIME} and
 * is checked against the database on every request.
 */
@Service
public class AuthService {

    static final Duration SESSION_LIFETIME = Duration.ofDays(30);

    /** PostgreSQL: unique_violation. */
    private static final String UNIQUE_VIOLATION = "23505";

    /** A signed-in session handed to the app. */
    public record Session(String token, Instant expiresAt, Account user) {
    }

    private final Jdbc jdbc;
    private final PasswordHasher hasher;
    private final LoginThrottle throttle;
    /** Compared against when the address is unknown, so both failures take as long. */
    private final String decoyHash;

    AuthService(Jdbc jdbc, PasswordHasher hasher, LoginThrottle throttle) {
        this.jdbc = jdbc;
        this.hasher = hasher;
        this.throttle = throttle;
        this.decoyHash = hasher.hash("silentvoix-decoy-password");
    }

    /** A new ordinary user (never an admin) with a password, signed in on the calling device. */
    public Session register(String rawEmail, String password, String rawName, String platform, String appVersion) {
        String email = Credentials.email(rawEmail);
        String hash = hasher.hash(Credentials.newPassword(password));
        String name = Credentials.displayName(rawName);
        String devicePlatform = Credentials.platform(platform);
        String version = Credentials.appVersion(appVersion);
        return jdbc.transaction(connection -> {
            UUID userId;
            try (PreparedStatement insert = Jdbc.prepare(connection,
                    "INSERT INTO app_user (display_name) VALUES (?) RETURNING id", name);
                 ResultSet rows = insert.executeQuery()) {
                rows.next();
                userId = rows.getObject(1, UUID.class);
            }
            try {
                Jdbc.update(connection, "INSERT INTO user_identity (user_id, provider, subject) VALUES (?, 'email', ?)",
                        userId, email);
            } catch (SQLException e) {
                if (UNIQUE_VIOLATION.equals(e.getSQLState())) {
                    throw new ApiException(HttpStatus.CONFLICT, "email_taken");
                }
                throw e;
            }
            Jdbc.update(connection, "INSERT INTO password_credential (user_id, password_hash) VALUES (?, ?)", userId, hash);
            Jdbc.update(connection, "INSERT INTO user_settings (user_id) VALUES (?)", userId);
            return openSession(connection, new Account(userId, email, name, "user"), devicePlatform, version);
        });
    }

    /**
     * Signs in with the address's password. A wrong password and an unknown address get the same
     * answer; a locked account is named only to someone who knows its password.
     */
    public Session login(String rawEmail, String password, String platform, String appVersion) {
        String email;
        try {
            email = Credentials.email(rawEmail);
        } catch (ApiException e) {
            throw invalidCredentials();
        }
        String devicePlatform = Credentials.platform(platform);
        String version = Credentials.appVersion(appVersion);
        if (throttle.isBlocked(email)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "too_many_attempts");
        }
        return jdbc.transaction(connection -> {
            try (PreparedStatement query = Jdbc.prepare(connection, """
                    SELECT u.id, u.display_name, u.role, u.disabled_at IS NOT NULL AS locked, p.password_hash
                    FROM user_identity i
                    JOIN app_user u ON u.id = i.user_id
                    LEFT JOIN password_credential p ON p.user_id = u.id
                    WHERE i.provider = 'email' AND i.subject = ?""", email);
                 ResultSet rows = query.executeQuery()) {
                if (!rows.next()) {
                    hasher.matches(password, decoyHash);
                    throttle.recordFailure(email);
                    throw invalidCredentials();
                }
                // No password on file (an OTP-only account) still costs one comparison.
                String hash = rows.getString("password_hash");
                boolean correct = hasher.matches(password, hash != null ? hash : decoyHash) && hash != null;
                if (!correct) {
                    throttle.recordFailure(email);
                    throw invalidCredentials();
                }
                throttle.recordSuccess(email);
                if (rows.getBoolean("locked")) {
                    throw new ApiException(HttpStatus.FORBIDDEN, "account_disabled");
                }
                Account account = new Account(rows.getObject("id", UUID.class), email,
                        rows.getString("display_name"), rows.getString("role"));
                return openSession(connection, account, devicePlatform, version);
            }
        });
    }

    /** The user behind [token], or null if it is unknown, revoked, expired or the account is locked. */
    public CurrentUser authenticate(String token) {
        byte[] hash = SessionTokens.hash(token);
        return jdbc.transaction(connection -> {
            CurrentUser user;
            try (PreparedStatement query = Jdbc.prepare(connection, """
                    SELECT u.id, i.subject AS email, u.display_name, u.role, d.id AS device_id, t.id AS session_id
                    FROM refresh_token t
                    JOIN device d ON d.id = t.device_id
                    JOIN app_user u ON u.id = d.user_id
                    LEFT JOIN user_identity i ON i.user_id = u.id AND i.provider = 'email'
                    WHERE t.token_hash = ? AND t.revoked_at IS NULL AND t.expires_at > now()
                      AND u.disabled_at IS NULL""", (Object) hash);
                 ResultSet rows = query.executeQuery()) {
                if (!rows.next()) {
                    return null;
                }
                user = new CurrentUser(
                        new Account(rows.getObject("id", UUID.class), rows.getString("email"),
                                rows.getString("display_name"), rows.getString("role")),
                        rows.getObject("device_id", UUID.class),
                        rows.getObject("session_id", UUID.class));
            }
            // At most one write a minute per device, not one per request.
            Jdbc.update(connection,
                    "UPDATE device SET last_seen_at = now() WHERE id = ? AND last_seen_at < now() - interval '1 minute'",
                    user.deviceId());
            return user;
        });
    }

    public void logout(CurrentUser user) {
        jdbc.transaction(connection -> Jdbc.update(connection,
                "UPDATE refresh_token SET revoked_at = now() WHERE id = ? AND revoked_at IS NULL", user.sessionId()));
    }

    private static Session openSession(Connection connection, Account account, String platform, String appVersion)
            throws SQLException {
        UUID deviceId;
        try (PreparedStatement insert = Jdbc.prepare(connection,
                "INSERT INTO device (user_id, platform, app_version) VALUES (?, ?, ?) RETURNING id",
                account.id(), platform, appVersion);
             ResultSet rows = insert.executeQuery()) {
            rows.next();
            deviceId = rows.getObject(1, UUID.class);
        }
        String token = SessionTokens.newToken();
        Instant expiresAt = Instant.now().plus(SESSION_LIFETIME);
        Jdbc.update(connection, "INSERT INTO refresh_token (device_id, token_hash, expires_at) VALUES (?, ?, ?)",
                deviceId, SessionTokens.hash(token), expiresAt);
        return new Session(token, expiresAt, account);
    }

    private static ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "invalid_credentials");
    }
}
