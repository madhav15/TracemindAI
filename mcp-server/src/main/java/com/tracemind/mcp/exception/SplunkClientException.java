package com.tracemind.mcp.exception;

public class SplunkClientException extends RuntimeException {

    public SplunkClientException(String message) {
        super(message);
    }

    public SplunkClientException(String message, Throwable cause) {
        super(message, cause);
    }
}