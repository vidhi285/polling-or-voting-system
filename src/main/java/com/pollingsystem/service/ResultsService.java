package com.pollingsystem.service;

import com.pollingsystem.exception.NotFoundException;
import com.pollingsystem.model.Poll;
import com.pollingsystem.model.PollResults;
import com.pollingsystem.model.PollStats;
import com.pollingsystem.repository.PollRepository;
import com.pollingsystem.repository.StatisticsRepository;
import com.pollingsystem.repository.VoteRepository;

public class ResultsService {
    private final PollRepository polls;
    private final VoteRepository votes;
    private final StatisticsRepository statistics;

    public ResultsService(PollRepository polls, VoteRepository votes, StatisticsRepository statistics) {
        this.polls = polls;
        this.votes = votes;
        this.statistics = statistics;
    }

    public PollResults getResults(String pollId) {
        Poll poll = polls.findById(pollId).orElseThrow(() -> new NotFoundException("Poll not found."));
        return ResultCalculator.calculate(poll, votes.countByOption(pollId));
    }

    public PollStats getStats() {
        return statistics.stats();
    }
}
