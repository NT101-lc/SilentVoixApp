package com.silentvoix.backend.database;

import java.sql.SQLException;

/**
 * A write broke a unique constraint (PostgreSQL {@code 23505}). The transaction can no longer
 * commit, so the caller should turn this into a refusal and let the transaction roll back.
 */
public class DuplicateKeyException extends DatabaseException {

    static final String UNIQUE_VIOLATION = "23505";

    DuplicateKeyException(SQLException cause) {
        super(cause);
    }
}
