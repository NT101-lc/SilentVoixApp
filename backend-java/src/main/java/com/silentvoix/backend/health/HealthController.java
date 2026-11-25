package com.silentvoix.backend.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class HealthController {

    private final HealthService health;

    HealthController(HealthService health) {
        this.health = health;
    }

    /** Always 200 while the process is serving; readiness is in the body (see {@link HealthService}). */
    @GetMapping("/health")
    HealthResponse health() {
        return health.check();
    }
}
