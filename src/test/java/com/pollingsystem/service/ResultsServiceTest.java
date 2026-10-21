package com.pollingsystem.service;

import static org.junit.jupiter.api.Assertions.*;

import com.pollingsystem.exception.NotFoundException;
import com.pollingsystem.model.OptionResult;
import com.pollingsystem.model.Poll;
import com.pollingsystem.model.PollResults;
import com.pollingsystem.model.PollStats;
import com.pollingsystem.support.InMemoryPollRepository;
import com.pollingsystem.support.InMemoryStatisticsRepository;
import com.pollingsystem.support.InMemoryVoteRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Results and statistics (PVS-11, PVS-18). Written after the implementation (not TDD). */
class ResultsServiceTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-25T08:00:00Z"), ZoneOffset.UTC);
    private InMemoryPollRepository polls;
    private InMemoryVoteRepository votes;
    private PollService pollService;
    private VoteService voteService;
    private ResultsService results;

    @BeforeEach
    void setUp() {
        polls = new InMemoryPollRepository();
        votes = new InMemoryVoteRepository();
        pollService = new PollService(polls, clock);
        voteService = new VoteService(polls, votes, clock);
        results = new ResultsService(polls, votes, new InMemoryStatisticsRepository(polls, votes));
    }

    private void vote(Poll poll, String voter, int option) {
        voteService.castVote(poll.id(), voter, poll.options().get(option).id());
    }

    @Test
    void zeroVotesGivesZeroPercentWithoutDividingByZero() {
        Poll poll = pollService.createPoll("Q?", List.of("A", "B", "C"));
        PollResults r = results.getResults(poll.id());
        assertEquals(0, r.totalVotes());
        assertEquals(3, r.options().size());
        r.options().forEach(o -> {
            assertEquals(0, o.votes());
            assertEquals(0.0, o.percentage());
        });
    }

    @Test
    void calculatesCountsAndPercentages() {
        Poll poll = pollService.createPoll("Q?", List.of("A", "B"));
        vote(poll, "v1", 0);
        vote(poll, "v2", 0);
        vote(poll, "v3", 0);
        vote(poll, "v4", 1);
        PollResults r = results.getResults(poll.id());
        assertEquals(4, r.totalVotes());
        assertEquals(3, r.options().get(0).votes());
        assertEquals(75.0, r.options().get(0).percentage());
        assertEquals(1, r.options().get(1).votes());
        assertEquals(25.0, r.options().get(1).percentage());
    }

    @Test
    void roundsToOneDecimalHalfUp() {
        Poll poll = pollService.createPoll("Q?", List.of("A", "B", "C"));
        vote(poll, "v1", 0);
        vote(poll, "v2", 1);
        vote(poll, "v3", 1);
        PollResults r = results.getResults(poll.id());
        assertEquals(33.3, r.options().get(0).percentage());
        assertEquals(66.7, r.options().get(1).percentage());
        assertEquals(0.0, r.options().get(2).percentage());
    }

    @Test
    void keepsOptionOrderAndStableIds() {
        Poll poll = pollService.createPoll("Q?", List.of("Zebra", "Apple"));
        PollResults r = results.getResults(poll.id());
        assertEquals(List.of("Zebra", "Apple"), r.options().stream().map(OptionResult::text).toList());
        assertEquals(poll.options().get(0).id(), r.options().get(0).optionId());
    }

    @Test
    void ignoresCountsForUnknownOptions() {
        Poll poll = pollService.createPoll("Q?", List.of("A", "B"));
        PollResults r = ResultCalculator.calculate(poll, Map.of(poll.options().get(0).id(), 2L, "ghost", 99L));
        assertEquals(2, r.totalVotes());
        assertEquals(100.0, r.options().get(0).percentage());
    }

    @Test
    void closedPollStillShowsItsResults() {
        Poll poll = pollService.createPoll("Q?", List.of("A", "B"));
        vote(poll, "v1", 1);
        pollService.closePoll(poll.id());
        PollResults r = results.getResults(poll.id());
        assertEquals("CLOSED", r.status());
        assertEquals(1, r.totalVotes());
        assertEquals(100.0, r.options().get(1).percentage());
    }

    @Test
    void unknownPollIsNotFound() {
        assertThrows(NotFoundException.class, () -> results.getResults("nope"));
    }

    @Test
    void statsReflectRealData() {
        assertEquals(new PollStats(0, 0, 0, 0), results.getStats());
        Poll a = pollService.createPoll("A?", List.of("1", "2"));
        Poll b = pollService.createPoll("B?", List.of("1", "2"));
        vote(a, "v1", 0);
        vote(b, "v1", 1);
        vote(b, "v2", 1);
        pollService.closePoll(b.id());
        assertEquals(new PollStats(2, 1, 1, 3), results.getStats());
    }
}
