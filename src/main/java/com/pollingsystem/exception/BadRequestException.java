package com.pollingsystem.exception;

public class BadRequestException extends ApiException {
    public BadRequestException(String code, String message) {
        super(400, code, message, null);
    }
}
