package com.pollingsystem.dto;

import com.pollingsystem.model.Poll;
import java.util.List;

/** What the browser sees for a poll. Instants are ISO-8601 strings. */
public record PollView(String id, String question, List<OptionView> options, String status,
                       boolean active, String createdAt, String closedAt) {

    public static PollView from(Poll poll) {
        return new PollView(
                poll.id(),
                poll.question(),
                poll.options().stream().map(o -> new OptionView(o.id(), o.text())).toList(),
                poll.status().name(),
                poll.isActive(),
                poll.createdAt().toString(),
                poll.closedAt() == null ? null : poll.closedAt().toString());
    }
}
