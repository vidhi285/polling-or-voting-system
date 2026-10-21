package com.pollingsystem.service;

import com.pollingsystem.exception.ConflictException;
import com.pollingsystem.exception.NotFoundException;
import com.pollingsystem.exception.ValidationException;
import com.pollingsystem.model.Poll;
import com.pollingsystem.model.PollOption;
import com.pollingsystem.model.PollStatus;
import com.pollingsystem.repository.PollRepository;
import com.pollingsystem.util.Ids;
import java.time.Clock;
import java.util.List;

/** Poll use-cases. Depends only on the PollRepository abstraction (Dependency Inversion). */
public class PollService {
    private final PollRepository polls;
    private final Clock clock;

    public PollService(PollRepository polls, Clock clock) {
        this.polls = polls;
        this.clock = clock;
    }

    public Poll createPoll(String question, List<String> options) {
        List<String> errors = PollValidator.validate(question, options);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        List<PollOption> pollOptions = options.stream()
                .map(text -> new PollOption(Ids.newOptionId(), text.trim()))
                .toList();
        Poll poll = new Poll(Ids.newId(), question.trim(), pollOptions, PollStatus.ACTIVE, clock.instant(), null);
        polls.save(poll);
        return poll;
    }

    public List<Poll> listPolls() {
        return polls.findAll();
    }

    public Poll getPoll(String id) {
        return polls.findById(id).orElseThrow(() -> new NotFoundException("Poll not found."));
    }

    /** Closes an active poll. Existing votes are kept; new votes are rejected afterwards. */
    public Poll closePoll(String id) {
        Poll poll = getPoll(id);
        if (!poll.isActive()) {
            throw new ConflictException("POLL_ALREADY_CLOSED", "This poll is already closed.");
        }
        if (!polls.closeIfActive(id, clock.instant())) {
            // someone closed it between our read and our update
            throw new ConflictException("POLL_ALREADY_CLOSED", "This poll is already closed.");
        }
        return getPoll(id);
    }
}
