package com.pollingsystem.repository;

public interface HealthChecker {
    /** Performs a real round trip to the database. */
    boolean isDatabaseAvailable();
}
