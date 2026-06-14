package com.tracemindai.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApplicationException {
    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND.value());
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause, "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND.value());
    }
}
