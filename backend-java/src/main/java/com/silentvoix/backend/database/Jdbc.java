package com.silentvoix.backend.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import javax.sql.DataSource;

import com.silentvoix.backend.api.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Runs plain JDBC work in one transaction. No database configured, Neon unreachable or any other
 * unexpected SQL error becomes {@link ApiException#databaseUnavailable()} (503); only the SQLState
 * is logged, since driver messages can name the host or database.
 */
@Component
public class Jdbc {

    private static final Logger log = LoggerFactory.getLogger(Jdbc.class);

    /** Work done on one connection; return a value or throw. */
    @FunctionalInterface
    public interface Work<T> {
        T run(Connection connection) throws SQLException;
    }

    private final ObjectProvider<DataSource> dataSource;

    public Jdbc(ObjectProvider<DataSource> dataSource) {
        this.dataSource = dataSource;
    }

    public <T> T transaction(Work<T> work) {
        DataSource source = dataSource.getIfAvailable();
        if (source == null) {
            throw ApiException.databaseUnavailable();
        }
        try (Connection connection = source.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            log.warn("Database call failed (SQLState {})", e.getSQLState());
            throw ApiException.databaseUnavailable();
        }
    }

    /** A statement with [params] bound in order. */
    public static PreparedStatement prepare(Connection connection, String sql, Object... params) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) {
            Object param = params[i];
            statement.setObject(i + 1, param instanceof Instant instant ? OffsetDateTime.ofInstant(instant, ZoneOffset.UTC) : param);
        }
        return statement;
    }

    public static int update(Connection connection, String sql, Object... params) throws SQLException {
        try (PreparedStatement statement = prepare(connection, sql, params)) {
            return statement.executeUpdate();
        }
    }

    public static long count(Connection connection, String sql, Object... params) throws SQLException {
        try (PreparedStatement statement = prepare(connection, sql, params); ResultSet rows = statement.executeQuery()) {
            rows.next();
            return rows.getLong(1);
        }
    }

    /** A timestamptz column as an Instant, or null. */
    public static Instant instant(ResultSet rows, String column) throws SQLException {
        OffsetDateTime value = rows.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}
