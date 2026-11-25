package com.silentvoix.backend.database;

import java.sql.SQLException;

/** Lets tests in other packages raise what {@link Jdbc} throws on a unique violation. */
public final class DuplicateKeyExceptions {

    private DuplicateKeyExceptions() {
    }

    public static DuplicateKeyException of(SQLException cause) {
        return new DuplicateKeyException(cause);
    }
}
