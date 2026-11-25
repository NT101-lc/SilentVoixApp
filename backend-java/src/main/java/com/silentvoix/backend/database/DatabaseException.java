package com.silentvoix.backend.database;

import java.sql.SQLException;

/**
 * An SQL failure inside a repository, unchecked so repository interfaces stay free of JDBC types.
 * Only the SQLState is kept: driver messages can name the host or database.
 */
public class DatabaseException extends RuntimeException {

    private final String sqlState;

    DatabaseException(SQLException cause) {
        super("SQLState " + cause.getSQLState(), null, false, false);
        this.sqlState = cause.getSQLState();
    }

    public String sqlState() {
        return sqlState;
    }
}
