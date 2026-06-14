package com.tracemindai.preprocessor.exception;

import com.tracemindai.common.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class PreprocessingException extends ApplicationException {
    public PreprocessingException(String message) {
        super(message, "PREPROCESSING_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    public PreprocessingException(String message, Throwable cause) {
        super(message, cause, "PREPROCESSING_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
