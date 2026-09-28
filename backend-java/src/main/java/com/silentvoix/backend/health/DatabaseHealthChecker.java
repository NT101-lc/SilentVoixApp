package com.silentvoix.backend.health;

import java.sql.Connection;
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
            return DatabaseHealth.up((System.nanoTime() - startedAt) / 1_000_000);
        } catch (SQLException e) {
            log.warn("Database health check failed: {}", e.getMessage());
            // The driver message can include host names; expose only the SQL state.
            String state = e.getSQLState();
            return DatabaseHealth.down(state != null ? "Connection failed (SQLState " + state + ")" : "Connection failed");
        }
    }
}
