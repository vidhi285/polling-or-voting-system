package com.pollingsystem.repository;

import com.pollingsystem.model.Poll;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Persistence operations for polls. Implementations translate storage errors to DataAccessException. */
public interface PollRepository {
    void save(Poll poll);

    Optional<Poll> findById(String id);

    /** All polls, newest first. */
    List<Poll> findAll();

    /** Atomically moves an ACTIVE poll to CLOSED. Returns false if it was not active (or not found). */
    boolean closeIfActive(String id, Instant closedAt);
}
