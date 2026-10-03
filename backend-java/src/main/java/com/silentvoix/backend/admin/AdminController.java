package com.silentvoix.backend.admin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.silentvoix.backend.api.ApiException;
import com.silentvoix.backend.auth.CurrentUser;
import com.silentvoix.backend.database.Jdbc;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code /api/v1/admin}: admins only (the session guard answers 403 to anyone else). The overview,
 * account management (role, lock) and the feedback inbox.
 */
@RestController
@RequestMapping("/api/v1/admin")
class AdminController {

    private static final Set<String> ROLES = Set.of("user", "admin");
    private static final int LIST_LIMIT = 500;

    record Overview(long users, long admins, long lockedUsers, long newUsersThisWeek, long activeUsersThisWeek,
                    long openFeedback, long totalFeedback) {
    }

    record AdminUser(UUID id, String email, String displayName, String role, boolean locked, Instant createdAt,
                     Instant lastSeenAt) {
    }

    /** Either field may be left out. */
    record UserChange(String role, Boolean locked) {
    }

    record FeedbackItem(UUID id, String kind, String message, Instant createdAt, Instant resolvedAt,
                        String authorEmail, String authorName) {
    }

    record FeedbackChange(Boolean resolved) {
    }

    private final Jdbc jdbc;

    AdminController(Jdbc jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/overview")
    Overview overview() {
        return jdbc.transaction(connection -> new Overview(
                Jdbc.count(connection, "SELECT count(*) FROM app_user"),
                Jdbc.count(connection, "SELECT count(*) FROM app_user WHERE role = 'admin'"),
                Jdbc.count(connection, "SELECT count(*) FROM app_user WHERE disabled_at IS NOT NULL"),
                Jdbc.count(connection, "SELECT count(*) FROM app_user WHERE created_at > now() - interval '7 days'"),
                Jdbc.count(connection, "SELECT count(DISTINCT user_id) FROM device WHERE last_seen_at > now() - interval '7 days'"),
                Jdbc.count(connection, "SELECT count(*) FROM feedback WHERE resolved_at IS NULL"),
                Jdbc.count(connection, "SELECT count(*) FROM feedback")));
    }

    @GetMapping("/users")
    List<AdminUser> users() {
        return jdbc.transaction(connection -> {
            List<AdminUser> users = new ArrayList<>();
            try (PreparedStatement query = Jdbc.prepare(connection, USERS_SQL + " ORDER BY u.created_at DESC LIMIT ?", LIST_LIMIT);
                 ResultSet rows = query.executeQuery()) {
                while (rows.next()) {
                    users.add(adminUser(rows));
                }
            }
            return users;
        });
    }

    @PatchMapping("/users/{id}")
    AdminUser changeUser(CurrentUser admin, @PathVariable String id, @RequestBody UserChange change) {
        UUID target = uuid(id);
        if (change.role() != null && !ROLES.contains(change.role())) {
            throw ApiException.badRequest("invalid_role");
        }
        if (target.equals(admin.id())) {
            // Keeps the last admin from locking or demoting themselves out of the app.
            throw new ApiException(HttpStatus.CONFLICT, "cannot_change_self");
        }
        return jdbc.transaction(connection -> {
            if (Jdbc.count(connection, "SELECT count(*) FROM app_user WHERE id = ?", target) == 0) {
                throw ApiException.notFound();
            }
            if (change.role() != null) {
                Jdbc.update(connection, "UPDATE app_user SET role = ? WHERE id = ?", change.role(), target);
            }
            if (Boolean.TRUE.equals(change.locked())) {
                Jdbc.update(connection, "UPDATE app_user SET disabled_at = coalesce(disabled_at, now()) WHERE id = ?", target);
                // Signed out everywhere at once, not when their sessions expire.
                Jdbc.update(connection, """
                        UPDATE refresh_token SET revoked_at = now()
                        WHERE revoked_at IS NULL AND device_id IN (SELECT id FROM device WHERE user_id = ?)""", target);
            } else if (Boolean.FALSE.equals(change.locked())) {
                Jdbc.update(connection, "UPDATE app_user SET disabled_at = NULL WHERE id = ?", target);
            }
            return findUser(connection, target);
        });
    }

    /** Newest first; {@code status=open} leaves out what was already handled. */
    @GetMapping("/feedback")
    List<FeedbackItem> feedback(@RequestParam(defaultValue = "all") String status) {
        String filter = "open".equals(status) ? " WHERE f.resolved_at IS NULL" : "";
        return jdbc.transaction(connection -> {
            List<FeedbackItem> items = new ArrayList<>();
            try (PreparedStatement query = Jdbc.prepare(connection,
                    FEEDBACK_SQL + filter + " ORDER BY f.created_at DESC LIMIT ?", LIST_LIMIT);
                 ResultSet rows = query.executeQuery()) {
                while (rows.next()) {
                    items.add(feedbackItem(rows));
                }
            }
            return items;
        });
    }

    @PatchMapping("/feedback/{id}")
    FeedbackItem changeFeedback(@PathVariable String id, @RequestBody FeedbackChange change) {
        UUID target = uuid(id);
        return jdbc.transaction(connection -> {
            if (change.resolved() != null) {
                Jdbc.update(connection,
                        "UPDATE feedback SET resolved_at = CASE WHEN ? THEN coalesce(resolved_at, now()) END WHERE id = ?",
                        change.resolved(), target);
            }
            try (PreparedStatement query = Jdbc.prepare(connection, FEEDBACK_SQL + " WHERE f.id = ?", target);
                 ResultSet rows = query.executeQuery()) {
                if (!rows.next()) {
                    throw ApiException.notFound();
                }
                return feedbackItem(rows);
            }
        });
    }

    private static final String USERS_SQL = """
            SELECT u.id, i.subject AS email, u.display_name, u.role, u.disabled_at IS NOT NULL AS locked, u.created_at,
                   (SELECT max(d.last_seen_at) FROM device d WHERE d.user_id = u.id) AS last_seen_at
            FROM app_user u
            LEFT JOIN user_identity i ON i.user_id = u.id AND i.provider = 'email'""";

    private static final String FEEDBACK_SQL = """
            SELECT f.id, f.kind, f.message, f.created_at, f.resolved_at, i.subject AS author_email, u.display_name AS author_name
            FROM feedback f
            LEFT JOIN app_user u ON u.id = f.user_id
            LEFT JOIN user_identity i ON i.user_id = u.id AND i.provider = 'email'""";

    private static AdminUser findUser(Connection connection, UUID id) throws SQLException {
        try (PreparedStatement query = Jdbc.prepare(connection, USERS_SQL + " WHERE u.id = ?", id);
             ResultSet rows = query.executeQuery()) {
            rows.next();
            return adminUser(rows);
        }
    }

    private static AdminUser adminUser(ResultSet rows) throws SQLException {
        return new AdminUser(rows.getObject("id", UUID.class), rows.getString("email"), rows.getString("display_name"),
                rows.getString("role"), rows.getBoolean("locked"), Jdbc.instant(rows, "created_at"),
                Jdbc.instant(rows, "last_seen_at"));
    }

    private static FeedbackItem feedbackItem(ResultSet rows) throws SQLException {
        return new FeedbackItem(rows.getObject("id", UUID.class), rows.getString("kind"), rows.getString("message"),
                Jdbc.instant(rows, "created_at"), Jdbc.instant(rows, "resolved_at"), rows.getString("author_email"),
                rows.getString("author_name"));
    }

    /** A malformed id names nothing, so it is a 404 like an unknown one. */
    private static UUID uuid(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw ApiException.notFound();
        }
    }
}
