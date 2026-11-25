package com.silentvoix.backend.admin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.silentvoix.backend.database.Jdbc;
import org.springframework.stereotype.Repository;

@Repository
class JdbcAdminUserRepository implements AdminUserRepository {

    private static final String SELECT = """
            SELECT u.id, i.subject AS email, u.display_name, u.role, u.disabled_at IS NOT NULL AS locked, u.created_at,
                   (SELECT max(d.last_seen_at) FROM device d WHERE d.user_id = u.id) AS last_seen_at
            FROM app_user u
            LEFT JOIN user_identity i ON i.user_id = u.id AND i.provider = 'email'""";

    private final Jdbc jdbc;

    JdbcAdminUserRepository(Jdbc jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<AdminUser> list(int limit) {
        return jdbc.list(SELECT + " ORDER BY u.created_at DESC LIMIT ?", JdbcAdminUserRepository::user, limit);
    }

    @Override
    public Optional<AdminUser> find(UUID id) {
        return jdbc.one(SELECT + " WHERE u.id = ?", JdbcAdminUserRepository::user, id);
    }

    @Override
    public void setRole(UUID id, String role) {
        jdbc.update("UPDATE app_user SET role = ? WHERE id = ?", role, id);
    }

    @Override
    public void lock(UUID id) {
        jdbc.update("UPDATE app_user SET disabled_at = coalesce(disabled_at, now()) WHERE id = ?", id);
    }

    @Override
    public void unlock(UUID id) {
        jdbc.update("UPDATE app_user SET disabled_at = NULL WHERE id = ?", id);
    }

    @Override
    public Counts counts() {
        return new Counts(
                jdbc.count("SELECT count(*) FROM app_user"),
                jdbc.count("SELECT count(*) FROM app_user WHERE role = 'admin'"),
                jdbc.count("SELECT count(*) FROM app_user WHERE disabled_at IS NOT NULL"),
                jdbc.count("SELECT count(*) FROM app_user WHERE created_at > now() - interval '7 days'"),
                jdbc.count("SELECT count(DISTINCT user_id) FROM device WHERE last_seen_at > now() - interval '7 days'"));
    }

    private static AdminUser user(ResultSet rows) throws SQLException {
        return new AdminUser(rows.getObject("id", UUID.class), rows.getString("email"), rows.getString("display_name"),
                rows.getString("role"), rows.getBoolean("locked"), Jdbc.instant(rows, "created_at"),
                Jdbc.instant(rows, "last_seen_at"));
    }
}
