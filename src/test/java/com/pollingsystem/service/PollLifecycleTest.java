package com.pollingsystem.service;

import static org.junit.jupiter.api.Assertions.*;

import com.pollingsystem.exception.ConflictException;
import com.pollingsystem.exception.NotFoundException;
import com.pollingsystem.model.Poll;
import com.pollingsystem.model.PollStatus;
import com.pollingsystem.support.InMemoryPollRepository;
import com.pollingsystem.support.InMemoryVoteRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Listing, retrieval and closing (PVS-19, PVS-22). Written after the implementation (not TDD). */
class PollLifecycleTest {
    private final Instant created = Instant.parse("2026-10-20T10:00:00Z");
    private final Instant closed = Instant.parse("2026-10-21T10:00:00Z");
    private InMemoryPollRepository polls;
    private InMemoryVoteRepository votes;

    @BeforeEach
    void setUp() {
        polls = new InMemoryPollRepository();
        votes = new InMemoryVoteRepository();
    }

    private PollService serviceAt(Instant now) {
        return new PollService(polls, Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void listsNewestPollsFirst() {
        Poll older = serviceAt(created).createPoll("Older?", List.of("A", "B"));
        Poll newer = serviceAt(created.plusSeconds(60)).createPoll("Newer?", List.of("A", "B"));
        assertEquals(List.of(newer.id(), older.id()), serviceAt(created).listPolls().stream().map(Poll::id).toList());
    }

    @Test
    void emptyDatabaseListsNoPolls() {
        assertTrue(serviceAt(created).listPolls().isEmpty());
    }

    @Test
    void getPollThrowsNotFoundForUnknownId() {
        assertThrows(NotFoundException.class, () -> serviceAt(created).getPoll("nope"));
    }

    @Test
    void closePollChangesStatusAndRecordsTime() {
        Poll poll = serviceAt(created).createPoll("Q?", List.of("A", "B"));
        Poll result = serviceAt(closed).closePoll(poll.id());
        assertEquals(PollStatus.CLOSED, result.status());
        assertEquals(closed, result.closedAt());
        assertEquals(PollStatus.CLOSED, polls.findById(poll.id()).orElseThrow().status());
    }

    @Test
    void closePollKeepsExistingVotes() {
        PollService service = serviceAt(created);
        Poll poll = service.createPoll("Q?", List.of("A", "B"));
        new VoteService(polls, votes, Clock.fixed(created, ZoneOffset.UTC)).castVote(poll.id(), "v1", poll.options().get(0).id());
        service.closePoll(poll.id());
        assertEquals(1L, votes.countByOption(poll.id()).get(poll.options().get(0).id()));
    }

    @Test
    void closingTwiceIsAConflict() {
        PollService service = serviceAt(created);
        Poll poll = service.createPoll("Q?", List.of("A", "B"));
        service.closePoll(poll.id());
        ConflictException ex = assertThrows(ConflictException.class, () -> service.closePoll(poll.id()));
        assertEquals("POLL_ALREADY_CLOSED", ex.code());
    }

    @Test
    void closingUnknownPollIsNotFound() {
        assertThrows(NotFoundException.class, () -> serviceAt(created).closePoll("nope"));
    }
}
