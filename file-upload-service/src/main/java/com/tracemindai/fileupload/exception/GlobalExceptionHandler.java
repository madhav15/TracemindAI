package com.tracemindai.fileupload.exception;

import com.tracemindai.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<ApiResponse<Void>> handleFileUploadException(FileUploadException ex) {
        log.error("FileUploadException: {} - {}", ex.getErrorCode(), ex.getMessage(), ex);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
            .code(ex.getHttpStatus())
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now().toString())
            .build();

        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        log.error("File size exceeded", ex);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
            .code(HttpStatus.PAYLOAD_TOO_LARGE.value())
            .message("File size exceeds maximum allowed size")
            .timestamp(LocalDateTime.now().toString())
            .build();

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
            .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .message("An unexpected error occurred")
            .timestamp(LocalDateTime.now().toString())
            .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
