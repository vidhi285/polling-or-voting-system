package com.pollingsystem.repository;

import com.pollingsystem.model.PollStats;

/** Read-only dashboard numbers, kept separate so other repositories stay small. */
public interface StatisticsRepository {
    PollStats stats();
}
