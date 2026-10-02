package com.silentvoix.backend.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

/** Runs the real migration scripts against a real, empty PostgreSQL 17 database. */
class SchemaMigratorTest {

    private final SchemaMigrator migrator = new SchemaMigrator();

    @Test
    void anEmptyDatabaseIsMigratedToTheLatestScript() {
        try (HikariDataSource dataSource = TestDatabase.fresh().dataSource()) {
            SchemaMigrator.Outcome outcome = migrator.migrate(dataSource);

            assertThat(outcome.applied()).isEqualTo(3);
            assertThat(outcome.version()).isEqualTo(MigrationScripts.latestVersion());
        }
    }

    @Test
    void runningAgainAppliesNothing() {
        try (HikariDataSource dataSource = TestDatabase.fresh().dataSource()) {
            migrator.migrate(dataSource);

            SchemaMigrator.Outcome again = migrator.migrate(dataSource);

            assertThat(again.applied()).isZero();
            assertThat(again.version()).isEqualTo(MigrationScripts.latestVersion());
        }
    }

    @Test
    void theSeedHoldsTheSevenStockGesturesAndTheirActiveModel() throws SQLException {
        try (HikariDataSource dataSource = TestDatabase.fresh().dataSource()) {
            migrator.migrate(dataSource);

            assertThat(strings(dataSource, "SELECT slug FROM sign WHERE is_active ORDER BY slug"))
                    .containsExactly("cho_toi_hoi", "dong_y", "dung_lai", "khong_dong_y", "toi_yeu_ban", "tuyet_voi", "xin_chao");
            assertThat(strings(dataSource, "SELECT name || ':' || version FROM model_version WHERE is_active"))
                    .containsExactly("mediapipe_gesture_recognizer:1");
            // Every label the stock model can output maps to one of those signs.
            assertThat(strings(dataSource, """
                    SELECT l.label FROM model_label l
                    JOIN model_version m ON m.id = l.model_version_id
                    JOIN sign s ON s.id = l.sign_id
                    WHERE m.is_active ORDER BY l.label"""))
                    .containsExactly("Closed_Fist", "ILoveYou", "Open_Palm", "Pointing_Up", "Thumb_Down", "Thumb_Up", "Victory");
        }
    }

    private static List<String> strings(HikariDataSource dataSource, String sql) throws SQLException {
        List<String> values = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            while (rows.next()) {
                values.add(rows.getString(1));
            }
        }
        return values;
    }
}
