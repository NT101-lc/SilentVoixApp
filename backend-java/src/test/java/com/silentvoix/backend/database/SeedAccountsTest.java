package com.silentvoix.backend.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

/** scripts/seed_accounts.sql, run as an operator would run it against a migrated database. */
class SeedAccountsTest {

    private static final Path SCRIPT = Path.of("scripts/seed_accounts.sql");

    private static void runSeed(HikariDataSource db) throws SQLException, IOException {
        try (Connection connection = db.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(Files.readString(SCRIPT));
        }
    }

    private static List<String> rows(HikariDataSource db, String sql) throws SQLException {
        List<String> values = new ArrayList<>();
        try (Connection connection = db.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) {
                values.add(result.getString(1));
            }
        }
        return values;
    }

    private static final String ACCOUNTS = """
            SELECT i.subject || ' ' || u.role || ' ' || (i.verified_at IS NOT NULL)
            FROM app_user u JOIN user_identity i ON i.user_id = u.id
            WHERE i.provider = 'email' ORDER BY i.subject""";

    @Test
    void createsOneAdminAndOneUserWithVerifiedEmails() throws Exception {
        try (HikariDataSource db = TestDatabase.fresh().dataSource()) {
            new SchemaMigrator().migrate(db);

            runSeed(db);

            assertThat(rows(db, ACCOUNTS)).containsExactly(
                    "admin@silentvoix.local admin true",
                    "user@silentvoix.local user true");
            // Each comes with default settings, like any user the app would create.
            assertThat(rows(db, "SELECT count(*) FROM user_settings")).containsExactly("2");
        }
    }

    @Test
    void runningItAgainChangesNothing() throws Exception {
        try (HikariDataSource db = TestDatabase.fresh().dataSource()) {
            new SchemaMigrator().migrate(db);

            runSeed(db);
            runSeed(db);

            assertThat(rows(db, "SELECT count(*) FROM app_user")).containsExactly("2");
            assertThat(rows(db, ACCOUNTS)).hasSize(2);
        }
    }

    @Test
    void anExistingAccountWithThatEmailIsReusedAndGivenItsRole() throws Exception {
        try (HikariDataSource db = TestDatabase.fresh().dataSource()) {
            new SchemaMigrator().migrate(db);
            try (Connection connection = db.getConnection(); Statement statement = connection.createStatement()) {
                statement.execute("""
                        WITH u AS (INSERT INTO app_user DEFAULT VALUES RETURNING id)
                        INSERT INTO user_identity (user_id, provider, subject) SELECT id, 'email', 'admin@silentvoix.local' FROM u""");
            }

            runSeed(db);

            assertThat(rows(db, "SELECT count(*) FROM app_user")).containsExactly("2");
            assertThat(rows(db, ACCOUNTS)).contains("admin@silentvoix.local admin true");
        }
    }
}
