package com.silentvoix.backend.health;

/**
 * Body of {@code GET /api/v1/health}.
 *
 * @param status    {@code UP} when the service and database are ready, {@code DEGRADED} when the
 *                  service is running but the database is down or not configured
 * @param timestamp ISO-8601 instant of the check
 */
public record HealthResponse(String status, String service, String timestamp, DatabaseHealth database) {
}
