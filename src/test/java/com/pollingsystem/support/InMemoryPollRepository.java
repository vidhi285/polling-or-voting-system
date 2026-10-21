package com.pollingsystem.support;

import com.pollingsystem.model.Poll;
import com.pollingsystem.model.PollStatus;
import com.pollingsystem.repository.PollRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Thread-safe fake used by unit tests. */
public class InMemoryPollRepository implements PollRepository {
    private final Map<String, Poll> polls = new LinkedHashMap<>();

    @Override
    public synchronized void save(Poll poll) {
        polls.put(poll.id(), poll);
    }

    @Override
    public synchronized Optional<Poll> findById(String id) {
        return Optional.ofNullable(polls.get(id));
    }

    @Override
    public synchronized List<Poll> findAll() {
        List<Poll> all = new ArrayList<>(polls.values());
        all.sort(Comparator.comparing(Poll::createdAt).reversed());
        return all;
    }

    @Override
    public synchronized boolean closeIfActive(String id, Instant closedAt) {
        Poll p = polls.get(id);
        if (p == null || !p.isActive()) return false;
        polls.put(id, new Poll(p.id(), p.question(), p.options(), PollStatus.CLOSED, p.createdAt(), closedAt));
        return true;
    }

    public synchronized int size() {
        return polls.size();
    }
}
