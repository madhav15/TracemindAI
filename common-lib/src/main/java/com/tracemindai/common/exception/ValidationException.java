package com.tracemindai.common.exception;

import org.springframework.http.HttpStatus;

public class ValidationException extends ApplicationException {
    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR", HttpStatus.BAD_REQUEST.value());
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause, "VALIDATION_ERROR", HttpStatus.BAD_REQUEST.value());
    }
}
