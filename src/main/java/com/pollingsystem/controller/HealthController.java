package com.pollingsystem.controller;

import com.pollingsystem.dto.HealthView;
import com.pollingsystem.repository.HealthChecker;
import java.time.Clock;

/** Reports real readiness: the database is pinged on every call. */
public class HealthController {
    private final HealthChecker checker;
    private final Clock clock;

    public HealthController(HealthChecker checker, Clock clock) {
        this.checker = checker;
        this.clock = clock;
    }

    public ApiResponse health() {
        boolean dbUp = checker.isDatabaseAvailable();
        HealthView view = new HealthView(dbUp ? "UP" : "DOWN", "UP", dbUp ? "UP" : "DOWN", clock.instant().toString());
        return new ApiResponse(dbUp ? 200 : 503, view);
    }
}
