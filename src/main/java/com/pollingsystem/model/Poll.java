package com.pollingsystem.model;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record Poll(String id, String question, List<PollOption> options,
                   PollStatus status, Instant createdAt, Instant closedAt) {

    public Poll {
        options = List.copyOf(options);
    }

    public boolean isActive() {
        return status == PollStatus.ACTIVE;
    }

    public Optional<PollOption> findOption(String optionId) {
        return options.stream().filter(o -> o.id().equals(optionId)).findFirst();
    }
}
