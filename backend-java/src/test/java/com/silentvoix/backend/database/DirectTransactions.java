package com.silentvoix.backend.database;

import java.util.function.Supplier;

/** Runs work straight away, for service tests with in-memory repositories. Counts the calls. */
public class DirectTransactions implements Transactions {

    public int opened;

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        opened++;
        return work.get();
    }
}
