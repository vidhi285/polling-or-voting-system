package com.pollingsystem.exception;

public class ConflictException extends ApiException {
    public ConflictException(String code, String message) {
        super(409, code, message, null);
    }
}
