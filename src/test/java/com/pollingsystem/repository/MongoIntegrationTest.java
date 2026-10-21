package com.pollingsystem.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.pollingsystem.config.AppConfig;
import com.pollingsystem.exception.DataAccessException;
import com.pollingsystem.exception.DuplicateVoteException;
import com.pollingsystem.model.Poll;
import com.pollingsystem.model.PollStats;
import com.pollingsystem.model.PollStatus;
import com.pollingsystem.model.Vote;
import com.pollingsystem.repository.mongo.MongoConnection;
import com.pollingsystem.repository.mongo.MongoPollRepository;
import com.pollingsystem.repository.mongo.MongoStatisticsRepository;
import com.pollingsystem.repository.mongo.MongoVoteRepository;
import com.pollingsystem.service.PollService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Needs a reachable MongoDB (default mongodb://127.0.0.1:27017, or MONGODB_URI). Uses a throw-away
 * database that is dropped afterwards. If MongoDB is not reachable the tests are SKIPPED, not passed.
 * Run with:  mvn verify -Pintegration
 */
@Tag("integration")
class MongoIntegrationTest {
    private static MongoConnection connection;
    private static AppConfig config;
    private static MongoPollRepository polls;
    private static MongoVoteRepository votes;

    @BeforeAll
    static void connect() {
        String uri = System.getenv().getOrDefault("MONGODB_URI", AppConfig.DEFAULT_MONGO_URI);
        config = new AppConfig(uri, "polling_voting_test_" + UUID.randomUUID().toString().substring(0, 8), 8080, "127.0.0.1");
        try {
            connection = MongoConnection.connect(config);
        } catch (DataAccessException e) {
            assumeTrue(false, "MongoDB is not reachable at " + config.safeMongoUri() + " - integration tests skipped");
        }
        polls = new MongoPollRepository(connection.polls());
        votes = new MongoVoteRepository(connection.votes());
    }

    @AfterAll
    static void cleanUp() {
        if (connection != null) {
            connection.database().drop();
            connection.close();
        }
    }

    private Poll newPoll() {
        return new PollService(polls, Clock.systemUTC()).createPoll("Integration poll?", List.of("A", "B"));
    }

    @Test
    void startupCreatesCollectionsAndUniqueVoteIndex() {
        List<String> names = new java.util.ArrayList<>();
        for (String n : connection.database().listCollectionNames()) names.add(n);
        assertTrue(names.contains("polls"));
        assertTrue(names.contains("votes"));
        boolean unique = false;
        for (Document idx : connection.votes().listIndexes()) {
            if ("uniq_poll_voter".equals(idx.getString("name"))) unique = Boolean.TRUE.equals(idx.get("unique"));
        }
        assertTrue(unique, "unique (pollId, voterId) index must exist");
        assertTrue(connection.isDatabaseAvailable());
    }

    @Test
    void initialisationIsIdempotent() {
        try (MongoConnection second = MongoConnection.connect(config)) {
            assertTrue(second.isDatabaseAvailable());
        }
    }

    @Test
    void pollRoundTripAndClose() {
        Poll poll = newPoll();
        Poll loaded = polls.findById(poll.id()).orElseThrow();
        assertEquals(poll.question(), loaded.question());
        assertEquals(poll.options(), loaded.options());
        assertEquals(PollStatus.ACTIVE, loaded.status());
        assertTrue(polls.closeIfActive(poll.id(), Instant.now()));
        assertFalse(polls.closeIfActive(poll.id(), Instant.now()), "second close must report no change");
        assertEquals(PollStatus.CLOSED, polls.findById(poll.id()).orElseThrow().status());
        assertTrue(polls.findAll().stream().anyMatch(p -> p.id().equals(poll.id())));
    }

    @Test
    void uniqueIndexRejectsDuplicateVote() {
        Poll poll = newPoll();
        String opt = poll.options().get(0).id();
        votes.insert(new Vote(UUID.randomUUID().toString(), poll.id(), "voter-1", opt, Instant.now()));
        assertThrows(DuplicateVoteException.class,
                () -> votes.insert(new Vote(UUID.randomUUID().toString(), poll.id(), "voter-1", opt, Instant.now())));
        assertEquals(1L, votes.countByOption(poll.id()).get(opt));
    }

    @Test
    void concurrentDuplicateVotesStoreExactlyOne() throws Exception {
        Poll poll = newPoll();
        String opt = poll.options().get(0).id();
        ExecutorService pool = Executors.newFixedThreadPool(16);
        AtomicInteger accepted = new AtomicInteger();
        try {
            List<Future<?>> futures = new java.util.ArrayList<>();
            for (int i = 0; i < 32; i++) {
                futures.add(pool.submit(() -> {
                    try {
                        votes.insert(new Vote(UUID.randomUUID().toString(), poll.id(), "racer", opt, Instant.now()));
                        accepted.incrementAndGet();
                    } catch (DuplicateVoteException ignored) {
                        // expected for all but one
                    }
                }));
            }
            for (Future<?> f : futures) f.get();
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, accepted.get());
        assertEquals(1L, votes.countByOption(poll.id()).get(opt));
    }

    @Test
    void statisticsCountRealDocuments() {
        PollStats before = new MongoStatisticsRepository(connection.polls(), connection.votes()).stats();
        Poll poll = newPoll();
        votes.insert(new Vote(UUID.randomUUID().toString(), poll.id(), "stats-voter", poll.options().get(0).id(), Instant.now()));
        PollStats after = new MongoStatisticsRepository(connection.polls(), connection.votes()).stats();
        assertEquals(before.totalPolls() + 1, after.totalPolls());
        assertEquals(before.totalVotes() + 1, after.totalVotes());
        assertEquals(after.totalPolls(), after.activePolls() + after.closedPolls());
    }
}
