package com.silentvoix.backend.auth;

import java.util.UUID;

/**
 * A signed-in person as the app sees them.
 *
 * @param email       null for an account with no e-mail identity
 * @param role        {@code user} or {@code admin}
 */
public record Account(UUID id, String email, String displayName, String role) {
}
