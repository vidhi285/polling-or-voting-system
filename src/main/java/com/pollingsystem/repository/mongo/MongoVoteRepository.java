package com.pollingsystem.repository.mongo;

import static com.mongodb.client.model.Filters.eq;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.pollingsystem.exception.DataAccessException;
import com.pollingsystem.exception.DuplicateVoteException;
import com.pollingsystem.model.Vote;
import com.pollingsystem.repository.VoteRepository;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.bson.Document;

public class MongoVoteRepository implements VoteRepository {
    private final MongoCollection<Document> collection;

    public MongoVoteRepository(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    /**
     * Single insertOne: MongoDB's unique index on (pollId, voterId) decides atomically, so two
     * simultaneous duplicate requests can never both succeed.
     */
    @Override
    public void insert(Vote vote) {
        try {
            MongoGuard.run(() -> collection.insertOne(new Document("_id", vote.id())
                    .append("pollId", vote.pollId())
                    .append("voterId", vote.voterId())
                    .append("optionId", vote.optionId())
                    .append("castAt", Date.from(vote.castAt()))));
        } catch (DataAccessException e) {
            if (e.getCause() instanceof MongoWriteException w && w.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
                throw new DuplicateVoteException();
            }
            throw e;
        }
    }

    @Override
    public Map<String, Long> countByOption(String pollId) {
        return MongoGuard.call(() -> {
            Map<String, Long> counts = new HashMap<>();
            for (Document d : collection.aggregate(Arrays.asList(
                    Aggregates.match(eq("pollId", pollId)),
                    Aggregates.group("$optionId", Accumulators.sum("count", 1))))) {
                counts.put(d.getString("_id"), ((Number) d.get("count")).longValue());
            }
            return counts;
        });
    }
}
