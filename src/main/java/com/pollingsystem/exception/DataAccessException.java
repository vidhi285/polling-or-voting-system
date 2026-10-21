package com.pollingsystem.exception;

/** The database could not be reached or a database operation failed. Never carries credentials. */
public class DataAccessException extends ApiException {
    public DataAccessException(String message, Throwable cause) {
        super(503, "DATABASE_UNAVAILABLE", message, cause);
    }
}
