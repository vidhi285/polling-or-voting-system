package com.pollingsystem.repository.mongo;

import com.mongodb.MongoCommandException;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.pollingsystem.config.AppConfig;
import com.pollingsystem.exception.DataAccessException;
import com.pollingsystem.repository.HealthChecker;
import java.util.HashSet;
import java.util.Set;
import org.bson.Document;

/**
 * Owns the MongoClient. On connect it verifies connectivity with a real ping, then creates the
 * missing collections and indexes. Everything is idempotent, so it is safe on every start-up.
 */
public final class MongoConnection implements HealthChecker, AutoCloseable {
    public static final String POLLS = "polls";
    public static final String VOTES = "votes";
    public static final String UNIQUE_VOTE_INDEX = "uniq_poll_voter";

    private final MongoClient client;
    private final MongoDatabase database;

    private MongoConnection(MongoClient client, MongoDatabase database) {
        this.client = client;
        this.database = database;
    }

    /** Connects and initialises the database. Throws DataAccessException (without credentials) on failure. */
    public static MongoConnection connect(AppConfig config) {
        MongoClient client = null;
        try {
            client = MongoClients.create(withServerSelectionTimeout(config.mongoUri()));
            MongoDatabase db = client.getDatabase(config.databaseName());
            db.runCommand(new Document("ping", 1)); // real connection check
            initialize(db);
            return new MongoConnection(client, db);
        } catch (IllegalArgumentException e) {
            closeQuietly(client);
            throw new DataAccessException("The MongoDB connection string is not valid.", null);
        } catch (MongoException e) {
            closeQuietly(client);
            throw new DataAccessException("Cannot connect to MongoDB or initialise the database ("
                    + e.getClass().getSimpleName() + ").", e);
        }
    }

    /** Creates missing collections and indexes, then verifies the duplicate-vote index exists. */
    static void initialize(MongoDatabase db) {
        Set<String> existing = new HashSet<>();
        for (String name : db.listCollectionNames()) {
            existing.add(name);
        }
        for (String name : new String[] {POLLS, VOTES}) {
            if (!existing.contains(name)) {
                try {
                    db.createCollection(name);
                } catch (MongoCommandException e) {
                    if (e.getErrorCode() != 48) throw e; // 48 = NamespaceExists (created concurrently)
                }
            }
        }
        MongoCollection<Document> polls = db.getCollection(POLLS);
        MongoCollection<Document> votes = db.getCollection(VOTES);

        polls.createIndex(Indexes.descending("createdAt"), new IndexOptions().name("idx_polls_createdAt"));
        polls.createIndex(Indexes.ascending("status"), new IndexOptions().name("idx_polls_status"));
        votes.createIndex(Indexes.ascending("pollId", "voterId"),
                new IndexOptions().unique(true).name(UNIQUE_VOTE_INDEX));
        votes.createIndex(Indexes.ascending("pollId", "optionId"), new IndexOptions().name("idx_votes_poll_option"));

        if (!hasUniqueVoteIndex(votes)) {
            throw new DataAccessException("Initialisation failed: the unique vote index is missing.", null);
        }
    }

    static boolean hasUniqueVoteIndex(MongoCollection<Document> votes) {
        for (Document index : votes.listIndexes()) {
            if (UNIQUE_VOTE_INDEX.equals(index.getString("name")) && Boolean.TRUE.equals(index.get("unique"))) {
                return true;
            }
        }
        return false;
    }

    public MongoCollection<Document> polls() {
        return database.getCollection(POLLS);
    }

    public MongoCollection<Document> votes() {
        return database.getCollection(VOTES);
    }

    public MongoDatabase database() {
        return database;
    }

    @Override
    public boolean isDatabaseAvailable() {
        try {
            database.runCommand(new Document("ping", 1));
            return true;
        } catch (MongoException e) {
            return false;
        }
    }

    @Override
    public void close() {
        closeQuietly(client);
    }

    private static void closeQuietly(MongoClient c) {
        if (c != null) {
            try {
                c.close();
            } catch (RuntimeException ignored) {
                // nothing useful to do while shutting down
            }
        }
    }

    /** Fail fast (5 s) instead of the driver's 30 s default when MongoDB is not running. */
    static String withServerSelectionTimeout(String uri) {
        if (uri.contains("serverSelectionTimeoutMS")) return uri;
        String option = "serverSelectionTimeoutMS=5000";
        if (uri.contains("?")) return uri + "&" + option;
        int afterScheme = uri.indexOf("://") + 3;
        return uri.indexOf('/', afterScheme) >= 0 ? uri + "?" + option : uri + "/?" + option;
    }
}
