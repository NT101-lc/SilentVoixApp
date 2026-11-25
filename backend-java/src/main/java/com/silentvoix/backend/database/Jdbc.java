package com.silentvoix.backend.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import javax.sql.DataSource;

import com.silentvoix.backend.api.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Plain JDBC for the repository layer, and the {@link Transactions} services use. The connection
 * of the running transaction is bound to the thread, so repositories never pass it around.
 * No database configured, Neon unreachable or any other unexpected SQL error becomes
 * {@link ApiException#databaseUnavailable()} (503); only the SQLState is logged, since driver
 * messages can name the host or database.
 */
@Component
public class Jdbc implements Transactions {

    private static final Logger log = LoggerFactory.getLogger(Jdbc.class);

    /** Turns the current row into a value. */
    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rows) throws SQLException;
    }

    private final ObjectProvider<DataSource> dataSource;
    private final ThreadLocal<Connection> current = new ThreadLocal<>();

    public Jdbc(ObjectProvider<DataSource> dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        if (current.get() != null) {
            return work.get();
        }
        DataSource source = dataSource.getIfAvailable();
        if (source == null) {
            throw ApiException.databaseUnavailable();
        }
        try (Connection connection = source.getConnection()) {
            connection.setAutoCommit(false);
            current.set(connection);
            try {
                T result = work.get();
                connection.commit();
                return result;
            } catch (RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                current.remove();
            }
        } catch (SQLException e) {
            throw unavailable(e.getSQLState());
        } catch (DatabaseException e) {
            throw unavailable(e.sqlState());
        }
    }

    /** Every row of the query, mapped in order. */
    public <T> List<T> list(String sql, RowMapper<T> mapper, Object... params) {
        return run(sql, params, statement -> {
            try (ResultSet rows = statement.executeQuery()) {
                List<T> values = new ArrayList<>();
                while (rows.next()) {
                    values.add(mapper.map(rows));
                }
                return values;
            }
        });
    }

    /** The first row of the query, or empty. */
    public <T> Optional<T> one(String sql, RowMapper<T> mapper, Object... params) {
        return run(sql, params, statement -> {
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(mapper.map(rows)) : Optional.empty();
            }
        });
    }

    /** The first row of a query that always returns one ({@code INSERT ... RETURNING}, say). */
    public <T> T single(String sql, RowMapper<T> mapper, Object... params) {
        return one(sql, mapper, params).orElseThrow(() -> new IllegalStateException("Query returned no row"));
    }

    /** Rows changed. Throws {@link DuplicateKeyException} when a unique constraint is broken. */
    public int update(String sql, Object... params) {
        return run(sql, params, PreparedStatement::executeUpdate);
    }

    /** The first column of the first row, as a number ({@code SELECT count(*) ...}). */
    public long count(String sql, Object... params) {
        return single(sql, rows -> rows.getLong(1), params);
    }

    /** A timestamptz column as an Instant, or null. */
    public static Instant instant(ResultSet rows, String column) throws SQLException {
        OffsetDateTime value = rows.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    @FunctionalInterface
    private interface StatementWork<T> {
        T run(PreparedStatement statement) throws SQLException;
    }

    private <T> T run(String sql, Object[] params, StatementWork<T> work) {
        Connection connection = current.get();
        if (connection == null) {
            throw new IllegalStateException("Repository called outside Transactions.inTransaction");
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                Object param = params[i];
                statement.setObject(i + 1,
                        param instanceof Instant instant ? OffsetDateTime.ofInstant(instant, ZoneOffset.UTC) : param);
            }
            return work.run(statement);
        } catch (SQLException e) {
            throw DuplicateKeyException.UNIQUE_VIOLATION.equals(e.getSQLState())
                    ? new DuplicateKeyException(e)
                    : new DatabaseException(e);
        }
    }

    private static ApiException unavailable(String sqlState) {
        log.warn("Database call failed (SQLState {})", sqlState);
        return ApiException.databaseUnavailable();
    }
}
