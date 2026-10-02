package com.silentvoix.backend.health;

import java.io.PrintWriter;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import javax.sql.DataSource;

/**
 * Hand-rolled DataSource for health tests: hands out a connection that is (in)valid, or fails. A
 * healthy one answers the schema-version query with [schemaVersion], or, when that is null, fails
 * it the way PostgreSQL does when Flyway's history table does not exist (SQLState 42P01).
 */
final class FakeDataSource implements DataSource {

    private final boolean connectionValid;
    private final SQLException failure;
    private final String schemaVersion;
    private int openConnections;

    private FakeDataSource(boolean connectionValid, SQLException failure, String schemaVersion) {
        this.connectionValid = connectionValid;
        this.failure = failure;
        this.schemaVersion = schemaVersion;
    }

    /** Reachable, but no migration has ever run. */
    static FakeDataSource healthy() {
        return new FakeDataSource(true, null, null);
    }

    /** Reachable and migrated up to [version]. */
    static FakeDataSource migratedTo(String version) {
        return new FakeDataSource(true, null, version);
    }

    static FakeDataSource invalidConnection() {
        return new FakeDataSource(false, null, null);
    }

    static FakeDataSource failing(SQLException failure) {
        return new FakeDataSource(false, failure, null);
    }

    /** Connections obtained but not yet closed. */
    int openConnections() {
        return openConnections;
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (failure != null) {
            throw failure;
        }
        openConnections++;
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[] {Connection.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isValid" -> connectionValid;
                    case "prepareStatement", "createStatement" -> statement();
                    case "close" -> {
                        openConnections--;
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private Object statement() throws SQLException {
        if (schemaVersion == null) {
            throw new SQLException("relation \"flyway_schema_history\" does not exist", "42P01");
        }
        ResultSet rows = (ResultSet) Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(),
                new Class<?>[] {ResultSet.class},
                new java.lang.reflect.InvocationHandler() {
                    private boolean read;

                    @Override
                    public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                        return switch (method.getName()) {
                            case "next" -> {
                                boolean first = !read;
                                read = true;
                                yield first;
                            }
                            case "getString" -> schemaVersion;
                            case "close" -> null;
                            default -> throw new UnsupportedOperationException(method.getName());
                        };
                    }
                });
        return Proxy.newProxyInstance(
                PreparedStatement.class.getClassLoader(),
                new Class<?>[] {PreparedStatement.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "executeQuery" -> rows;
                    case "setQueryTimeout", "close" -> null;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return getConnection();
    }

    @Override
    public PrintWriter getLogWriter() {
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) {
    }

    @Override
    public void setLoginTimeout(int seconds) {
    }

    @Override
    public int getLoginTimeout() {
        return 0;
    }

    @Override
    public Logger getParentLogger() {
        return Logger.getGlobal();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLException("not a wrapper");
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return false;
    }
}
