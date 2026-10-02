package com.silentvoix.backend.health;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/** Checks whether a connection to the configured PostgreSQL (Neon) database can be obtained. */
@Component
class DatabaseHealthChecker {

    private static final Logger log = LoggerFactory.getLogger(DatabaseHealthChecker.class);

    private static final int VALIDATION_TIMEOUT_SECONDS = 5;

    private static final String SCHEMA_VERSION_SQL = """
            SELECT version FROM flyway_schema_history
            WHERE success AND version IS NOT NULL
            ORDER BY installed_rank DESC LIMIT 1""";

    /** PostgreSQL: relation does not exist (never migrated, so no Flyway history table). */
    private static final String UNDEFINED_TABLE = "42P01";

    private final ObjectProvider<DataSource> dataSource;

    DatabaseHealthChecker(ObjectProvider<DataSource> dataSource) {
        this.dataSource = dataSource;
    }

    DatabaseHealth check() {
        DataSource source = dataSource.getIfAvailable();
        if (source == null) {
            return DatabaseHealth.notConfigured();
        }

        long startedAt = System.nanoTime();
        try (Connection connection = source.getConnection()) {
            if (!connection.isValid(VALIDATION_TIMEOUT_SECONDS)) {
                return DatabaseHealth.down("Connection validation failed");
            }
            long latencyMs = (System.nanoTime() - startedAt) / 1_000_000;
            return DatabaseHealth.up(latencyMs, schemaVersion(connection));
        } catch (SQLException e) {
            log.warn("Database health check failed: {}", e.getMessage());
            // The driver message can include host names; expose only the SQL state.
            String state = e.getSQLState();
            return DatabaseHealth.down(state != null ? "Connection failed (SQLState " + state + ")" : "Connection failed");
        }
    }

    /** The latest successfully applied migration, or null if the schema was never migrated. */
    private static String schemaVersion(Connection connection) {
        try (PreparedStatement statement = connection.prepareStatement(SCHEMA_VERSION_SQL)) {
            statement.setQueryTimeout(VALIDATION_TIMEOUT_SECONDS);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getString(1) : null;
            }
        } catch (SQLException e) {
            if (!UNDEFINED_TABLE.equals(e.getSQLState())) {
                log.warn("Could not read the schema version (SQLState {})", e.getSQLState());
            }
            return null;
        }
    }
}
