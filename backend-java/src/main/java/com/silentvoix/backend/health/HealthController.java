package com.silentvoix.backend.health;

import java.time.Instant;
import java.util.Objects;

import com.silentvoix.backend.database.MigrationScripts;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class HealthController {

    private final DatabaseHealthChecker databaseHealthChecker;
    private final String serviceName;

    HealthController(DatabaseHealthChecker databaseHealthChecker,
                     @Value("${spring.application.name}") String serviceName) {
        this.databaseHealthChecker = databaseHealthChecker;
        this.serviceName = serviceName;
    }

    /**
     * Always 200 while the process is serving; readiness is in the body. {@code UP} needs the
     * database reachable and migrated to the latest script this build ships.
     */
    @GetMapping("/health")
    HealthResponse health() {
        DatabaseHealth database = databaseHealthChecker.check();
        boolean ready = database.status() == DatabaseHealth.Status.UP
                && Objects.equals(database.schemaVersion(), MigrationScripts.latestVersion());
        String status = ready ? "UP" : "DEGRADED";
        return new HealthResponse(status, serviceName, Instant.now().toString(), database);
    }
}
