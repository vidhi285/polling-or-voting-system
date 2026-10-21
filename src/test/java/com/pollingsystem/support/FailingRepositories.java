package com.pollingsystem.support;

import com.pollingsystem.exception.DataAccessException;
import com.pollingsystem.model.Poll;
import com.pollingsystem.model.Vote;
import com.pollingsystem.repository.PollRepository;
import com.pollingsystem.repository.VoteRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Repositories that behave like a database that has gone away. */
public final class FailingRepositories {
    private FailingRepositories() { }

    private static DataAccessException down() {
        return new DataAccessException("The database is currently unavailable.", null);
    }

    public static class Polls implements PollRepository {
        @Override public void save(Poll poll) { throw down(); }
        @Override public Optional<Poll> findById(String id) { throw down(); }
        @Override public List<Poll> findAll() { throw down(); }
        @Override public boolean closeIfActive(String id, Instant closedAt) { throw down(); }
    }

    public static class Votes implements VoteRepository {
        @Override public void insert(Vote vote) { throw down(); }
        @Override public Map<String, Long> countByOption(String pollId) { throw down(); }
    }
}
