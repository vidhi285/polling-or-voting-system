package com.pollingsystem.exception;

import java.util.List;

public class ValidationException extends ApiException {
    private final List<String> errors;

    public ValidationException(List<String> errors) {
        super(400, "VALIDATION_ERROR", String.join(" ", errors), null);
        this.errors = List.copyOf(errors);
    }

    public ValidationException(String error) {
        this(List.of(error));
    }

    public List<String> errors() { return errors; }
}
