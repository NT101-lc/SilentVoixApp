package com.silentvoix.backend.database;

import java.util.function.Supplier;

/**
 * Runs service-layer work in one database transaction. Repositories called inside it share the
 * transaction; a nested call joins the outer one. Services depend on this interface, not on JDBC.
 */
public interface Transactions {

    /**
     * Runs [work] and commits, or rolls back if it throws. Any database failure (none configured,
     * unreachable, an unexpected SQL error) surfaces as a 503 {@code database_unavailable}.
     */
    <T> T inTransaction(Supplier<T> work);

    /** {@link #inTransaction(Supplier)} for work with no result. */
    default void run(Runnable work) {
        inTransaction(() -> {
            work.run();
            return null;
        });
    }
}
