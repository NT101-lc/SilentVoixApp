package com.silentvoix.backend.health;

/**
 * Result of the database readiness check.
 *
 * @param latencyMs time to obtain and validate a connection, or {@code null} unless {@code UP}
 * @param error     short, credential-free reason, or {@code null} unless {@code DOWN}
 */
public record DatabaseHealth(Status status, Long latencyMs, String error) {

    public enum Status {
        UP,
        DOWN,
        NOT_CONFIGURED
    }

    static DatabaseHealth up(long latencyMs) {
        return new DatabaseHealth(Status.UP, latencyMs, null);
    }

    static DatabaseHealth down(String error) {
        return new DatabaseHealth(Status.DOWN, null, error);
    }

    static DatabaseHealth notConfigured() {
        return new DatabaseHealth(Status.NOT_CONFIGURED, null, null);
    }
}
