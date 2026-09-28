package com.silentvoix.backend.health;

import java.time.Instant;

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

    /** Always 200 while the process is serving; database readiness is reported in the body. */
    @GetMapping("/health")
    HealthResponse health() {
        DatabaseHealth database = databaseHealthChecker.check();
        String status = database.status() == DatabaseHealth.Status.UP ? "UP" : "DEGRADED";
        return new HealthResponse(status, serviceName, Instant.now().toString(), database);
    }
}
