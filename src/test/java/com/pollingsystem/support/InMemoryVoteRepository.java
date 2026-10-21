package com.pollingsystem.support;

import com.pollingsystem.exception.DuplicateVoteException;
import com.pollingsystem.model.Vote;
import com.pollingsystem.repository.VoteRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Fake that mimics the unique (pollId, voterId) index: insert is atomic and rejects duplicates. */
public class InMemoryVoteRepository implements VoteRepository {
    private final Set<String> uniqueKeys = new HashSet<>();
    private final List<Vote> votes = new ArrayList<>();

    @Override
    public synchronized void insert(Vote vote) {
        String key = vote.pollId() + "\u0000" + vote.voterId();
        if (!uniqueKeys.add(key)) {
            throw new DuplicateVoteException();
        }
        votes.add(vote);
    }

    @Override
    public synchronized Map<String, Long> countByOption(String pollId) {
        Map<String, Long> counts = new HashMap<>();
        for (Vote v : votes) {
            if (v.pollId().equals(pollId)) counts.merge(v.optionId(), 1L, Long::sum);
        }
        return counts;
    }

    public synchronized int size() {
        return votes.size();
    }
}
