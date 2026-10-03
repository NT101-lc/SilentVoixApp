package com.silentvoix.backend.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.silentvoix.backend.auth.PasswordHasher;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

/** scripts/seed_accounts.sql, run as an operator would run it against a migrated database. */
class SeedAccountsTest {

    private static final Path SCRIPT = Path.of("scripts/seed_accounts.sql");

    /** Runs the script after filling in the two passwords, as the operator is told to. */
    private static void runSeed(HikariDataSource db, String adminPassword, String userPassword) throws SQLException, IOException {
        execute(db, Files.readString(SCRIPT)
                .replace("<admin password>", adminPassword)
                .replace("<user password>", userPassword));
    }

    /** Like psql with ON_ERROR_STOP: a failure ends the script's open transaction, nothing stays. */
    private static void execute(HikariDataSource db, String script) throws SQLException {
        try (Connection connection = db.getConnection(); Statement statement = connection.createStatement()) {
            try {
                statement.execute(script);
            } catch (SQLException e) {
                statement.execute("ROLLBACK");
                throw e;
            }
        }
    }

    private static void runSeed(HikariDataSource db) throws SQLException, IOException {
        runSeed(db, "admin-pass-1", "user-pass-1");
    }

    private static String passwordHash(HikariDataSource db, String email) throws SQLException {
        List<String> hashes = rows(db, """
                SELECT p.password_hash FROM password_credential p
                JOIN user_identity i ON i.user_id = p.user_id
                WHERE i.provider = 'email' AND i.subject = '%s'""".formatted(email));
        return hashes.isEmpty() ? null : hashes.get(0);
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
    void eachAccountSignsInWithThePasswordFilledIn() throws Exception {
        try (HikariDataSource db = TestDatabase.fresh().dataSource()) {
            new SchemaMigrator().migrate(db);

            runSeed(db, "admin-pass-1", "user-pass-1");

            // pgcrypto's BCrypt, checked by the same code the sign-in endpoint uses.
            PasswordHasher hasher = new PasswordHasher();
            assertThat(hasher.matches("admin-pass-1", passwordHash(db, "admin@silentvoix.local"))).isTrue();
            assertThat(hasher.matches("user-pass-1", passwordHash(db, "user@silentvoix.local"))).isTrue();
            assertThat(hasher.matches("user-pass-1", passwordHash(db, "admin@silentvoix.local"))).isFalse();
        }
    }

    @Test
    void refusesToRunWithThePlaceholdersStillInPlace() throws Exception {
        try (HikariDataSource db = TestDatabase.fresh().dataSource()) {
            new SchemaMigrator().migrate(db);

            assertThatThrownBy(() -> execute(db, Files.readString(SCRIPT)))
                    .isInstanceOf(SQLException.class).hasMessageContaining("password");

            assertThat(rows(db, "SELECT count(*) FROM app_user")).containsExactly("0");
        }
    }

    @Test
    void refusesAPasswordShorterThanTheAppAllows() throws Exception {
        try (HikariDataSource db = TestDatabase.fresh().dataSource()) {
            new SchemaMigrator().migrate(db);

            assertThatThrownBy(() -> runSeed(db, "short", "user-pass-1")).isInstanceOf(SQLException.class);
            assertThat(rows(db, "SELECT count(*) FROM app_user")).containsExactly("0");
        }
    }

    @Test
    void runningAgainWithNewPasswordsReplacesThem() throws Exception {
        try (HikariDataSource db = TestDatabase.fresh().dataSource()) {
            new SchemaMigrator().migrate(db);
            runSeed(db, "admin-pass-1", "user-pass-1");

            runSeed(db, "admin-pass-2", "user-pass-2");

            assertThat(new PasswordHasher().matches("admin-pass-2", passwordHash(db, "admin@silentvoix.local"))).isTrue();
            assertThat(rows(db, "SELECT count(*) FROM password_credential")).containsExactly("2");
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
