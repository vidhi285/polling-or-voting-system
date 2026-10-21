package com.pollingsystem.support;

import com.pollingsystem.model.Poll;
import com.pollingsystem.model.PollStats;
import com.pollingsystem.repository.StatisticsRepository;
import java.util.List;

public class InMemoryStatisticsRepository implements StatisticsRepository {
    private final InMemoryPollRepository polls;
    private final InMemoryVoteRepository votes;

    public InMemoryStatisticsRepository(InMemoryPollRepository polls, InMemoryVoteRepository votes) {
        this.polls = polls;
        this.votes = votes;
    }

    @Override
    public PollStats stats() {
        List<Poll> all = polls.findAll();
        long active = all.stream().filter(Poll::isActive).count();
        return new PollStats(all.size(), active, all.size() - active, votes.size());
    }
}
