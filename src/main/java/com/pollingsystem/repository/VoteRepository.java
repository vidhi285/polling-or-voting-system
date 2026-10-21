package com.pollingsystem.repository;

import com.pollingsystem.exception.DuplicateVoteException;
import com.pollingsystem.model.Vote;
import java.util.Map;

public interface VoteRepository {
    /**
     * Stores a vote. Must be atomic: if the same voter already voted in the same poll it throws
     * {@link DuplicateVoteException} and stores nothing, even when two requests race.
     */
    void insert(Vote vote);

    /** Vote counts keyed by option id. Options without votes are simply absent. */
    Map<String, Long> countByOption(String pollId);
}
