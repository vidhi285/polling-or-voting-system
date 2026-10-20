package com.pollingsystem.repository.mongo;

import static com.mongodb.client.model.Filters.eq;

import com.mongodb.client.MongoCollection;
import com.pollingsystem.model.PollStats;
import com.pollingsystem.model.PollStatus;
import com.pollingsystem.repository.StatisticsRepository;
import org.bson.Document;

public class MongoStatisticsRepository implements StatisticsRepository {
    private final MongoCollection<Document> polls;
    private final MongoCollection<Document> votes;

    public MongoStatisticsRepository(MongoCollection<Document> polls, MongoCollection<Document> votes) {
        this.polls = polls;
        this.votes = votes;
    }

    @Override
    public PollStats stats() {
        return MongoGuard.call(() -> {
            long total = polls.countDocuments();
            long active = polls.countDocuments(eq("status", PollStatus.ACTIVE.name()));
            return new PollStats(total, active, total - active, votes.countDocuments());
        });
    }
}
