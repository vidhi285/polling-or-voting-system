package com.pollingsystem.service;

import static org.junit.jupiter.api.Assertions.*;

import com.pollingsystem.exception.DataAccessException;
import com.pollingsystem.exception.DuplicateVoteException;
import com.pollingsystem.exception.NotFoundException;
import com.pollingsystem.exception.PollClosedException;
import com.pollingsystem.exception.ValidationException;
import com.pollingsystem.model.Poll;
import com.pollingsystem.model.Vote;
import com.pollingsystem.support.FailingRepositories;
import com.pollingsystem.support.InMemoryPollRepository;
import com.pollingsystem.support.InMemoryVoteRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** TDD module 2: voting and duplicate-vote prevention (PVS-9, PVS-10, PVS-17, PVS-20). */
class VoteServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-24T09:30:00Z");
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private InMemoryPollRepository polls;
    private InMemoryVoteRepository votes;
    private PollService pollService;
    private VoteService voteService;
    private Poll poll;

    @BeforeEach
    void setUp() {
        polls = new InMemoryPollRepository();
        votes = new InMemoryVoteRepository();
        pollService = new PollService(polls, clock);
        voteService = new VoteService(polls, votes, clock);
        poll = pollService.createPoll("Best language?", List.of("Java", "Python", "Go"));
    }

    private String optionId(int index) {
        return poll.options().get(index).id();
    }

    @Test
    void recordsAValidVote() {
        Vote vote = voteService.castVote(poll.id(), "voter-1", optionId(0));
        assertEquals(poll.id(), vote.pollId());
        assertEquals("voter-1", vote.voterId());
        assertEquals(optionId(0), vote.optionId());
        assertEquals(NOW, vote.castAt());
        assertNotNull(vote.id());
        assertEquals(1, votes.size());
        assertEquals(1L, votes.countByOption(poll.id()).get(optionId(0)));
    }

    @Test
    void trimsVoterId() {
        Vote vote = voteService.castVote(poll.id(), "  voter-1  ", optionId(0));
        assertEquals("voter-1", vote.voterId());
        assertThrows(DuplicateVoteException.class, () -> voteService.castVote(poll.id(), "voter-1", optionId(1)));
    }

    @Test
    void rejectsSecondVoteFromSameVoterInSamePoll() {
        voteService.castVote(poll.id(), "voter-1", optionId(0));
        assertThrows(DuplicateVoteException.class, () -> voteService.castVote(poll.id(), "voter-1", optionId(0)));
    }

    @Test
    void duplicateVoteForADifferentOptionIsAlsoRejectedAndChangesNothing() {
        voteService.castVote(poll.id(), "voter-1", optionId(0));
        assertThrows(DuplicateVoteException.class, () -> voteService.castVote(poll.id(), "voter-1", optionId(1)));
        assertEquals(1, votes.size());
        assertEquals(1L, votes.countByOption(poll.id()).get(optionId(0)));
        assertNull(votes.countByOption(poll.id()).get(optionId(1)));
    }

    @Test
    void differentVotersMayVoteInTheSamePoll() {
        voteService.castVote(poll.id(), "voter-1", optionId(0));
        voteService.castVote(poll.id(), "voter-2", optionId(0));
        assertEquals(2L, votes.countByOption(poll.id()).get(optionId(0)));
    }

    @Test
    void sameVoterMayVoteInDifferentPolls() {
        Poll other = pollService.createPoll("Other?", List.of("A", "B"));
        voteService.castVote(poll.id(), "voter-1", optionId(0));
        assertDoesNotThrow(() -> voteService.castVote(other.id(), "voter-1", other.options().get(0).id()));
    }

    @Test
    void rejectsVoteOnNonexistentPoll() {
        assertThrows(NotFoundException.class, () -> voteService.castVote("missing-poll", "voter-1", "opt_x"));
        assertEquals(0, votes.size());
    }

    @Test
    void rejectsVoteOnClosedPoll() {
        polls.closeIfActive(poll.id(), NOW);
        assertThrows(PollClosedException.class, () -> voteService.castVote(poll.id(), "voter-1", optionId(0)));
        assertEquals(0, votes.size());
    }

    @Test
    void rejectsOptionThatBelongsToAnotherPoll() {
        Poll other = pollService.createPoll("Other?", List.of("A", "B"));
        ValidationException ex = assertThrows(ValidationException.class,
                () -> voteService.castVote(poll.id(), "voter-1", other.options().get(0).id()));
        assertTrue(ex.errors().contains("Selected option does not belong to this poll."));
        assertEquals(0, votes.size());
    }

    @Test
    void rejectsInvalidOptionId() {
        assertThrows(ValidationException.class, () -> voteService.castVote(poll.id(), "voter-1", "nope"));
    }

    @Test
    void optionIsLookedUpByStableIdNotByPosition() {
        // Option text equal to a position number must not confuse the lookup.
        Poll numeric = pollService.createPoll("Pick", List.of("1", "0"));
        assertThrows(ValidationException.class, () -> voteService.castVote(numeric.id(), "voter-1", "0"));
        assertDoesNotThrow(() -> voteService.castVote(numeric.id(), "voter-1", numeric.options().get(1).id()));
    }

    @Test
    void rejectsMissingVoterId() {
        assertThrows(ValidationException.class, () -> voteService.castVote(poll.id(), null, optionId(0)));
        assertThrows(ValidationException.class, () -> voteService.castVote(poll.id(), "   ", optionId(0)));
    }

    @Test
    void rejectsMissingOptionId() {
        assertThrows(ValidationException.class, () -> voteService.castVote(poll.id(), "voter-1", null));
        assertThrows(ValidationException.class, () -> voteService.castVote(poll.id(), "voter-1", " "));
    }

    @Test
    void rejectsOverlongVoterId() {
        assertThrows(ValidationException.class, () -> voteService.castVote(poll.id(), "v".repeat(101), optionId(0)));
    }

    @Test
    void propagatesDatabaseFailureWhenReadingPoll() {
        VoteService broken = new VoteService(new FailingRepositories.Polls(), votes, clock);
        assertThrows(DataAccessException.class, () -> broken.castVote(poll.id(), "voter-1", optionId(0)));
    }

    @Test
    void propagatesDatabaseFailureWhenStoringVote() {
        VoteService broken = new VoteService(polls, new FailingRepositories.Votes(), clock);
        assertThrows(DataAccessException.class, () -> broken.castVote(poll.id(), "voter-1", optionId(0)));
    }

    @Test
    @Timeout(20)
    void concurrentDuplicateRequestsRecordExactlyOneVote() throws Exception {
        int threads = 24;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger duplicates = new AtomicInteger();
        try {
            List<Future<?>> futures = new java.util.ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    try {
                        voteService.castVote(poll.id(), "same-voter", optionId(0));
                        accepted.incrementAndGet();
                    } catch (DuplicateVoteException e) {
                        duplicates.incrementAndGet();
                    }
                    return null;
                }));
            }
            ready.await();
            go.countDown();
            for (Future<?> f : futures) f.get(10, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, accepted.get());
        assertEquals(threads - 1, duplicates.get());
        assertEquals(1, votes.size());
    }
}
