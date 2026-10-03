package com.silentvoix.backend.auth;

import java.util.UUID;

/**
 * The caller of a signed-in request, read fresh from the database on every call, so a role change
 * or a lock applies at once. A controller gets it by declaring a parameter of this type.
 *
 * @param sessionId the {@code refresh_token} row the request came with
 */
public record CurrentUser(Account account, UUID deviceId, UUID sessionId) {

    public static final String ROLE_ADMIN = "admin";

    public UUID id() {
        return account.id();
    }

    public boolean isAdmin() {
        return ROLE_ADMIN.equals(account.role());
    }
}
