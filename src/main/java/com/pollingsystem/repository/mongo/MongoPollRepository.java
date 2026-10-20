package com.pollingsystem.repository.mongo;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import com.pollingsystem.model.Poll;
import com.pollingsystem.model.PollOption;
import com.pollingsystem.model.PollStatus;
import com.pollingsystem.repository.PollRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.bson.Document;

public class MongoPollRepository implements PollRepository {
    private final MongoCollection<Document> collection;

    public MongoPollRepository(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    @Override
    public void save(Poll poll) {
        MongoGuard.run(() -> collection.insertOne(toDocument(poll)));
    }

    @Override
    public Optional<Poll> findById(String id) {
        return MongoGuard.call(() -> Optional.ofNullable(collection.find(eq("_id", id)).first()).map(MongoPollRepository::fromDocument));
    }

    @Override
    public List<Poll> findAll() {
        return MongoGuard.call(() -> {
            List<Poll> result = new ArrayList<>();
            for (Document d : collection.find().sort(Sorts.descending("createdAt"))) {
                result.add(fromDocument(d));
            }
            return result;
        });
    }

    @Override
    public boolean closeIfActive(String id, Instant closedAt) {
        return MongoGuard.call(() -> collection.updateOne(
                and(eq("_id", id), eq("status", PollStatus.ACTIVE.name())),
                Updates.combine(
                        Updates.set("status", PollStatus.CLOSED.name()),
                        Updates.set("closedAt", Date.from(closedAt)))
        ).getModifiedCount() == 1);
    }

    static Document toDocument(Poll poll) {
        List<Document> options = new ArrayList<>();
        for (PollOption o : poll.options()) {
            options.add(new Document("id", o.id()).append("text", o.text()));
        }
        return new Document("_id", poll.id())
                .append("question", poll.question())
                .append("options", options)
                .append("status", poll.status().name())
                .append("createdAt", Date.from(poll.createdAt()))
                .append("closedAt", poll.closedAt() == null ? null : Date.from(poll.closedAt()));
    }

    static Poll fromDocument(Document d) {
        List<PollOption> options = new ArrayList<>();
        for (Object item : (List<?>) d.get("options")) {
            Document o = (Document) item;
            options.add(new PollOption(o.getString("id"), o.getString("text")));
        }
        Date closed = d.getDate("closedAt");
        return new Poll(d.getString("_id"), d.getString("question"), options,
                PollStatus.valueOf(d.getString("status")),
                d.getDate("createdAt").toInstant(),
                closed == null ? null : closed.toInstant());
    }
}
