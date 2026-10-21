package com.pollingsystem.exception;

/** Base class for errors that map directly to an HTTP status and a stable error code. */
public abstract class ApiException extends RuntimeException {
    private final int status;
    private final String code;

    protected ApiException(int status, String code, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
    }

    public int status() { return status; }
    public String code() { return code; }
}
