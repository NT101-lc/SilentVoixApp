package com.silentvoix.backend.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.silentvoix.backend.database.Jdbc;
import org.springframework.stereotype.Repository;

@Repository
class JdbcSessionRepository implements SessionRepository {

    private final Jdbc jdbc;

    JdbcSessionRepository(Jdbc jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public UUID addDevice(UUID userId, String platform, String appVersion) {
        return jdbc.single("INSERT INTO device (user_id, platform, app_version) VALUES (?, ?, ?) RETURNING id",
                rows -> rows.getObject(1, UUID.class), userId, platform, appVersion);
    }

    @Override
    public void addSession(UUID deviceId, byte[] tokenHash, Instant expiresAt) {
        jdbc.update("INSERT INTO refresh_token (device_id, token_hash, expires_at) VALUES (?, ?, ?)",
                deviceId, tokenHash, expiresAt);
    }

    @Override
    public Optional<CurrentUser> findActive(byte[] tokenHash) {
        return jdbc.one("""
                SELECT u.id, i.subject AS email, u.display_name, u.role, d.id AS device_id, t.id AS session_id
                FROM refresh_token t
                JOIN device d ON d.id = t.device_id
                JOIN app_user u ON u.id = d.user_id
                LEFT JOIN user_identity i ON i.user_id = u.id AND i.provider = 'email'
                WHERE t.token_hash = ? AND t.revoked_at IS NULL AND t.expires_at > now()
                  AND u.disabled_at IS NULL""",
                rows -> new CurrentUser(
                        new Account(rows.getObject("id", UUID.class), rows.getString("email"),
                                rows.getString("display_name"), rows.getString("role")),
                        rows.getObject("device_id", UUID.class),
                        rows.getObject("session_id", UUID.class)),
                (Object) tokenHash);
    }

    @Override
    public void touchDevice(UUID deviceId) {
        jdbc.update("UPDATE device SET last_seen_at = now() WHERE id = ? AND last_seen_at < now() - interval '1 minute'",
                deviceId);
    }

    @Override
    public void revoke(UUID sessionId) {
        jdbc.update("UPDATE refresh_token SET revoked_at = now() WHERE id = ? AND revoked_at IS NULL", sessionId);
    }

    @Override
    public void revokeAllFor(UUID userId) {
        jdbc.update("""
                UPDATE refresh_token SET revoked_at = now()
                WHERE revoked_at IS NULL AND device_id IN (SELECT id FROM device WHERE user_id = ?)""", userId);
    }
}
