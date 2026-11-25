package com.silentvoix.backend.admin;

import java.time.Instant;
import java.util.UUID;

/** An account as an admin sees it. {@code lastSeenAt} is null if none of its devices ever called. */
public record AdminUser(UUID id, String email, String displayName, String role, boolean locked, Instant createdAt,
                        Instant lastSeenAt) {
}
