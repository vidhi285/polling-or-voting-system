package com.pollingsystem.exception;

public class PayloadTooLargeException extends ApiException {
    public PayloadTooLargeException(int maxBytes) {
        super(413, "PAYLOAD_TOO_LARGE", "Request body is larger than " + maxBytes + " bytes.", null);
    }
}
