package com.pollingsystem.service;

import static org.junit.jupiter.api.Assertions.*;

import com.pollingsystem.exception.DataAccessException;
import com.pollingsystem.exception.ValidationException;
import com.pollingsystem.model.Poll;
import com.pollingsystem.model.PollOption;
import com.pollingsystem.model.PollStatus;
import com.pollingsystem.support.FailingRepositories;
import com.pollingsystem.support.InMemoryPollRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** TDD module 1: poll creation and validation (PVS-7, PVS-8, PVS-16, PVS-21). */
class PollServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-20T10:00:00Z");
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private InMemoryPollRepository repo;
    private PollService service;

    @BeforeEach
    void setUp() {
        repo = new InMemoryPollRepository();
        service = new PollService(repo, clock);
    }

    private ValidationException rejected(String question, List<String> options) {
        ValidationException ex = assertThrows(ValidationException.class, () -> service.createPoll(question, options));
        assertEquals(0, repo.size(), "an invalid poll must not be stored");
        return ex;
    }

    @Test
    void rejectsNullQuestion() {
        assertTrue(rejected(null, List.of("Yes", "No")).errors().contains("Question is required."));
    }

    @Test
    void rejectsEmptyQuestion() {
        assertTrue(rejected("", List.of("Yes", "No")).errors().contains("Question is required."));
    }

    @Test
    void rejectsWhitespaceOnlyQuestion() {
        assertTrue(rejected("   \t ", List.of("Yes", "No")).errors().contains("Question is required."));
    }

    @Test
    void rejectsQuestionLongerThan200Characters() {
        String tooLong = "q".repeat(201);
        assertTrue(rejected(tooLong, List.of("Yes", "No")).errors().contains("Question must be at most 200 characters."));
    }

    @Test
    void acceptsQuestionOfExactly200Characters() {
        Poll poll = service.createPoll("q".repeat(200), List.of("Yes", "No"));
        assertEquals(200, poll.question().length());
    }

    @Test
    void rejectsNullOptionsList() {
        assertTrue(rejected("Lunch?", null).errors().contains("At least two options are required."));
    }

    @Test
    void rejectsFewerThanTwoOptions() {
        assertTrue(rejected("Lunch?", List.of("Pizza")).errors().contains("At least two options are required."));
    }

    @Test
    void rejectsEmptyOptionText() {
        assertTrue(rejected("Lunch?", List.of("Pizza", "")).errors().contains("Options must not be empty."));
    }

    @Test
    void rejectsWhitespaceOnlyOptionText() {
        assertTrue(rejected("Lunch?", List.of("Pizza", "   ")).errors().contains("Options must not be empty."));
    }

    @Test
    void rejectsNullOptionEntry() {
        List<String> options = new ArrayList<>();
        options.add("Pizza");
        options.add(null);
        assertTrue(rejected("Lunch?", options).errors().contains("Options must not be empty."));
    }

    @Test
    void rejectsDuplicateOptionsIgnoringCaseAndSurroundingSpaces() {
        assertTrue(rejected("Lunch?", List.of("Pizza", " pizza ")).errors().contains("Options must be unique."));
    }

    @Test
    void rejectsMoreThanTenOptions() {
        List<String> eleven = new ArrayList<>();
        for (int i = 1; i <= 11; i++) eleven.add("Option " + i);
        assertTrue(rejected("Pick one", eleven).errors().contains("A poll can have at most 10 options."));
    }

    @Test
    void rejectsOptionLongerThan100Characters() {
        assertTrue(rejected("Lunch?", List.of("a".repeat(101), "b")).errors()
                .contains("Each option must be at most 100 characters."));
    }

    @Test
    void reportsEveryProblemAtOnce() {
        ValidationException ex = rejected(" ", List.of("Only one"));
        assertTrue(ex.errors().contains("Question is required."));
        assertTrue(ex.errors().contains("At least two options are required."));
        assertEquals(2, ex.errors().size());
    }

    @Test
    void trimsQuestionAndOptions() {
        Poll poll = service.createPoll("  Best language?  ", List.of("  Java ", "Python  "));
        assertEquals("Best language?", poll.question());
        assertEquals(List.of("Java", "Python"), poll.options().stream().map(PollOption::text).toList());
    }

    @Test
    void createsActivePollWithCreatedTimestampFromClock() {
        Poll poll = service.createPoll("Best language?", List.of("Java", "Python"));
        assertEquals(PollStatus.ACTIVE, poll.status());
        assertEquals(NOW, poll.createdAt());
        assertNull(poll.closedAt());
        assertNotNull(poll.id());
        assertFalse(poll.id().isBlank());
    }

    @Test
    void giveEveryOptionAUniqueStableId() {
        Poll poll = service.createPoll("Best language?", List.of("Java", "Python", "Go"));
        List<String> ids = poll.options().stream().map(PollOption::id).toList();
        assertEquals(3, new HashSet<>(ids).size());
        ids.forEach(id -> assertTrue(id.startsWith("opt_")));
        // the stored poll has exactly the same ids (they do not depend on array positions)
        assertEquals(ids, repo.findById(poll.id()).orElseThrow().options().stream().map(PollOption::id).toList());
    }

    @Test
    void persistsValidPoll() {
        Poll poll = service.createPoll("Best language?", List.of("Java", "Python"));
        assertEquals(1, repo.size());
        assertEquals(poll, repo.findById(poll.id()).orElseThrow());
    }

    @Test
    void twoPollsGetDifferentIds() {
        Poll a = service.createPoll("A?", List.of("1", "2"));
        Poll b = service.createPoll("B?", List.of("1", "2"));
        assertNotEquals(a.id(), b.id());
    }

    @Test
    void propagatesDatabaseFailureInsteadOfPretendingSuccess() {
        PollService broken = new PollService(new FailingRepositories.Polls(), clock);
        assertThrows(DataAccessException.class, () -> broken.createPoll("Best language?", List.of("Java", "Python")));
    }
}
