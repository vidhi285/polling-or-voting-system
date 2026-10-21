package com.pollingsystem.dto;

import com.pollingsystem.model.Vote;

/** Confirmation of a vote. The voter id is deliberately not echoed back. */
public record VoteView(String message, String pollId, String optionId, String castAt) {
    public static VoteView from(Vote vote) {
        return new VoteView("Vote recorded.", vote.pollId(), vote.optionId(), vote.castAt().toString());
    }
}
