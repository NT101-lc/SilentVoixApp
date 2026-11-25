package com.silentvoix.backend.health;

import java.time.Clock;
import java.util.Objects;

import com.silentvoix.backend.database.MigrationScripts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Decides readiness: {@code UP} needs the database reachable and migrated to the latest script
 * this build ships; anything else is {@code DEGRADED}.
 */
@Service
class HealthService {

    private final DatabaseHealthChecker databaseHealthChecker;
    private final String serviceName;
    private final Clock clock;

    @Autowired
    HealthService(DatabaseHealthChecker databaseHealthChecker, @Value("${spring.application.name}") String serviceName) {
        this(databaseHealthChecker, serviceName, Clock.systemUTC());
    }

    HealthService(DatabaseHealthChecker databaseHealthChecker, String serviceName, Clock clock) {
        this.databaseHealthChecker = databaseHealthChecker;
        this.serviceName = serviceName;
        this.clock = clock;
    }

    HealthResponse check() {
        DatabaseHealth database = databaseHealthChecker.check();
        boolean ready = database.status() == DatabaseHealth.Status.UP
                && Objects.equals(database.schemaVersion(), MigrationScripts.latestVersion());
        return new HealthResponse(ready ? "UP" : "DEGRADED", serviceName, clock.instant().toString(), database);
    }
}
