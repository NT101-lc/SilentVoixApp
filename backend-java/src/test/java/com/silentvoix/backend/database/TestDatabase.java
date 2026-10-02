package com.silentvoix.backend.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * One PostgreSQL 17 container (the version Neon runs) for the whole test run. Each caller gets its
 * own empty database inside it, so tests never see each other's rows or migrations.
 * Testcontainers' reaper removes the container when the JVM exits.
 */
public final class TestDatabase {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");
    private static final AtomicInteger COUNTER = new AtomicInteger();

    static {
        POSTGRES.start();
    }

    /** Connection settings for one empty database. */
    public record Database(String jdbcUrl, String username, String password) {

        public HikariDataSource dataSource() {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username);
            config.setPassword(password);
            config.setMaximumPoolSize(3);
            return new HikariDataSource(config);
        }
    }

    private TestDatabase() {
    }

    public static Database fresh() {
        String name = "test_" + COUNTER.incrementAndGet();
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + name);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not create test database " + name, e);
        }
        String url = "jdbc:postgresql://" + POSTGRES.getHost() + ":" + POSTGRES.getMappedPort(5432) + "/" + name;
        return new Database(url, POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
