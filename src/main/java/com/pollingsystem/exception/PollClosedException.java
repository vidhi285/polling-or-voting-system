package com.pollingsystem.exception;

public class PollClosedException extends ConflictException {
    public PollClosedException() {
        super("POLL_CLOSED", "This poll is closed.");
    }
}
