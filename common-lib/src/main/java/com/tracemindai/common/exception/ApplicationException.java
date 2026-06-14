package com.tracemindai.common.exception;

public class ApplicationException extends RuntimeException {
    private final String errorCode;
    private final int httpStatus;

    public ApplicationException(String message, String errorCode, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public ApplicationException(String message, Throwable cause, String errorCode, int httpStatus) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
