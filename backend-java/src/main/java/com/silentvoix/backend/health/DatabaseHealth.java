package com.silentvoix.backend.health;

/**
 * Result of the database readiness check.
 *
 * @param latencyMs     time to obtain and validate a connection, or {@code null} unless {@code UP}
 * @param error         short, credential-free reason, or {@code null} unless {@code DOWN}
 * @param schemaVersion the latest applied migration, or {@code null} if none has run (or not {@code UP})
 */
public record DatabaseHealth(Status status, Long latencyMs, String error, String schemaVersion) {

    public enum Status {
        UP,
        DOWN,
        NOT_CONFIGURED
    }

    static DatabaseHealth up(long latencyMs, String schemaVersion) {
        return new DatabaseHealth(Status.UP, latencyMs, null, schemaVersion);
    }

    static DatabaseHealth down(String error) {
        return new DatabaseHealth(Status.DOWN, null, error, null);
    }

    static DatabaseHealth notConfigured() {
        return new DatabaseHealth(Status.NOT_CONFIGURED, null, null, null);
    }
}
