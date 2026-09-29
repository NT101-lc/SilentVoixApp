package com.silentvoix.backend.health;

import java.io.PrintWriter;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

import javax.sql.DataSource;

/** Hand-rolled DataSource for health tests: hands out a connection that is (in)valid, or fails. */
final class FakeDataSource implements DataSource {

    private final boolean connectionValid;
    private final SQLException failure;
    private int openConnections;

    private FakeDataSource(boolean connectionValid, SQLException failure) {
        this.connectionValid = connectionValid;
        this.failure = failure;
    }

    static FakeDataSource healthy() {
        return new FakeDataSource(true, null);
    }

    static FakeDataSource invalidConnection() {
        return new FakeDataSource(false, null);
    }

    static FakeDataSource failing(SQLException failure) {
        return new FakeDataSource(false, failure);
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
                    case "close" -> {
                        openConnections--;
                        yield null;
                    }
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
