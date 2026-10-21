package com.pollingsystem.exception;

public class MethodNotAllowedException extends ApiException {
    private final String allow;

    public MethodNotAllowedException(String allow) {
        super(405, "METHOD_NOT_ALLOWED", "This endpoint does not support that HTTP method.", null);
        this.allow = allow;
    }

    public String allow() { return allow; }
}
