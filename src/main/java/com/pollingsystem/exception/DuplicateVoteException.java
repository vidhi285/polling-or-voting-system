package com.pollingsystem.exception;

public class DuplicateVoteException extends ConflictException {
    public DuplicateVoteException() {
        super("DUPLICATE_VOTE", "You have already voted in this poll.");
    }
}
