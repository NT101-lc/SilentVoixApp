package com.silentvoix.backend.feedback;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.silentvoix.backend.database.Jdbc;
import org.springframework.stereotype.Repository;

@Repository
class JdbcFeedbackRepository implements FeedbackRepository {

    private static final String SELECT = """
            SELECT f.id, f.kind, f.message, f.created_at, f.resolved_at, i.subject AS author_email, u.display_name AS author_name
            FROM feedback f
            LEFT JOIN app_user u ON u.id = f.user_id
            LEFT JOIN user_identity i ON i.user_id = u.id AND i.provider = 'email'""";

    private final Jdbc jdbc;

    JdbcFeedbackRepository(Jdbc jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public UUID create(UUID userId, UUID deviceId, String kind, String message) {
        return jdbc.single("INSERT INTO feedback (user_id, device_id, kind, message) VALUES (?, ?, ?, ?) RETURNING id",
                rows -> rows.getObject(1, UUID.class), userId, deviceId, kind, message);
    }

    @Override
    public List<FeedbackItem> list(boolean openOnly, int limit) {
        String filter = openOnly ? " WHERE f.resolved_at IS NULL" : "";
        return jdbc.list(SELECT + filter + " ORDER BY f.created_at DESC LIMIT ?", JdbcFeedbackRepository::item, limit);
    }

    @Override
    public Optional<FeedbackItem> find(UUID id) {
        return jdbc.one(SELECT + " WHERE f.id = ?", JdbcFeedbackRepository::item, id);
    }

    @Override
    public void setResolved(UUID id, boolean resolved) {
        jdbc.update("UPDATE feedback SET resolved_at = CASE WHEN ? THEN coalesce(resolved_at, now()) END WHERE id = ?",
                resolved, id);
    }

    @Override
    public Counts counts() {
        return new Counts(
                jdbc.count("SELECT count(*) FROM feedback WHERE resolved_at IS NULL"),
                jdbc.count("SELECT count(*) FROM feedback"));
    }

    private static FeedbackItem item(ResultSet rows) throws SQLException {
        return new FeedbackItem(rows.getObject("id", UUID.class), rows.getString("kind"), rows.getString("message"),
                Jdbc.instant(rows, "created_at"), Jdbc.instant(rows, "resolved_at"), rows.getString("author_email"),
                rows.getString("author_name"));
    }
}
