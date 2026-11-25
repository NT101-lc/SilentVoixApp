package com.silentvoix.backend.auth;

import java.util.Optional;
import java.util.UUID;

import com.silentvoix.backend.database.Jdbc;
import org.springframework.stereotype.Repository;

@Repository
class JdbcAccountRepository implements AccountRepository {

    private final Jdbc jdbc;

    JdbcAccountRepository(Jdbc jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public UUID createUser(String displayName) {
        UUID id = jdbc.single("INSERT INTO app_user (display_name) VALUES (?) RETURNING id",
                rows -> rows.getObject(1, UUID.class), displayName);
        jdbc.update("INSERT INTO user_settings (user_id) VALUES (?)", id);
        return id;
    }

    @Override
    public void addEmailIdentity(UUID userId, String email) {
        jdbc.update("INSERT INTO user_identity (user_id, provider, subject) VALUES (?, 'email', ?)", userId, email);
    }

    @Override
    public void addPasswordHash(UUID userId, String passwordHash) {
        jdbc.update("INSERT INTO password_credential (user_id, password_hash) VALUES (?, ?)", userId, passwordHash);
    }

    @Override
    public Optional<LoginRecord> findByEmail(String email) {
        return jdbc.one("""
                SELECT u.id, u.display_name, u.role, u.disabled_at IS NOT NULL AS locked, p.password_hash
                FROM user_identity i
                JOIN app_user u ON u.id = i.user_id
                LEFT JOIN password_credential p ON p.user_id = u.id
                WHERE i.provider = 'email' AND i.subject = ?""",
                rows -> new LoginRecord(
                        new Account(rows.getObject("id", UUID.class), email, rows.getString("display_name"),
                                rows.getString("role")),
                        rows.getBoolean("locked"),
                        rows.getString("password_hash")),
                email);
    }
}
