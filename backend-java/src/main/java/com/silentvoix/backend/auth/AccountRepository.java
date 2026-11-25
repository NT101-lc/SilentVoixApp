package com.silentvoix.backend.auth;

import java.util.Optional;
import java.util.UUID;

/** Accounts and their e-mail + password sign-in. Called inside a service transaction. */
public interface AccountRepository {

    /** What signing in needs to know about an address. */
    record LoginRecord(Account account, boolean locked, String passwordHash) {
    }

    /** A new ordinary user (role {@code user}) with default settings; returns its id. */
    UUID createUser(String displayName);

    /** @throws com.silentvoix.backend.database.DuplicateKeyException if the address is taken */
    void addEmailIdentity(UUID userId, String email);

    void addPasswordHash(UUID userId, String passwordHash);

    /** Empty when no account signs in with [email]; the hash is null for an account with no password. */
    Optional<LoginRecord> findByEmail(String email);
}
