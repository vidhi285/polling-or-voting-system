package com.pollingsystem.service;

import com.pollingsystem.exception.NotFoundException;
import com.pollingsystem.exception.PollClosedException;
import com.pollingsystem.exception.ValidationException;
import com.pollingsystem.model.Poll;
import com.pollingsystem.model.Vote;
import com.pollingsystem.repository.PollRepository;
import com.pollingsystem.repository.VoteRepository;
import com.pollingsystem.util.Ids;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * Casting votes. Duplicate prevention is NOT done here with a check-then-insert (that would race);
 * it is delegated to the atomic {@link VoteRepository#insert(Vote)}, backed in MongoDB by a unique
 * compound index on (pollId, voterId).
 */
public class VoteService {
    public static final int MAX_VOTER_ID_LENGTH = 100;

    private final PollRepository polls;
    private final VoteRepository votes;
    private final Clock clock;

    public VoteService(PollRepository polls, VoteRepository votes, Clock clock) {
        this.polls = polls;
        this.votes = votes;
        this.clock = clock;
    }

    public Vote castVote(String pollId, String voterId, String optionId) {
        String voter = voterId == null ? "" : voterId.trim();
        String option = optionId == null ? "" : optionId.trim();
        List<String> errors = new ArrayList<>();
        if (voter.isEmpty()) errors.add("Voter ID is required.");
        else if (voter.length() > MAX_VOTER_ID_LENGTH) errors.add("Voter ID must be at most " + MAX_VOTER_ID_LENGTH + " characters.");
        if (option.isEmpty()) errors.add("Option is required.");
        if (!errors.isEmpty()) throw new ValidationException(errors);

        Poll poll = polls.findById(pollId).orElseThrow(() -> new NotFoundException("Poll not found."));
        if (!poll.isActive()) throw new PollClosedException();
        if (poll.findOption(option).isEmpty()) {
            throw new ValidationException("Selected option does not belong to this poll.");
        }

        Vote vote = new Vote(Ids.newId(), poll.id(), voter, option, clock.instant());
        votes.insert(vote); // throws DuplicateVoteException atomically
        return vote;
    }
}
