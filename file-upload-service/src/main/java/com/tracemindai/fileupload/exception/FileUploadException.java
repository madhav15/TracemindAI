package com.tracemindai.fileupload.exception;

import com.tracemindai.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class FileUploadException extends ApplicationException {
    public FileUploadException(String message) {
        super(message, "FILE_UPLOAD_ERROR", HttpStatus.BAD_REQUEST.value());
    }

    public FileUploadException(String message, Throwable cause) {
        super(message, cause, "FILE_UPLOAD_ERROR", HttpStatus.BAD_REQUEST.value());
    }

    public FileUploadException(String message, String errorCode, int httpStatus) {
        super(message, errorCode, httpStatus);
    }

    public FileUploadException(String message, Throwable cause, String errorCode, int httpStatus) {
        super(message, cause, errorCode, httpStatus);
    }
}
