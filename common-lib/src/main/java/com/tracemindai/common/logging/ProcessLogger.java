package com.tracemindai.common.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tracemindai.common.logging.dto.ProcessLog;
import com.tracemindai.common.logging.enums.ProcessAction;
import com.tracemindai.common.logging.enums.ProcessStage;
import com.tracemindai.common.logging.enums.ProcessStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class ProcessLogger {

    private static final Logger log = LoggerFactory.getLogger(ProcessLogger.class);

    private final ObjectMapper mapper;

    public ProcessLogger() {
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // Accept an externally configured ObjectMapper (useful in Spring context)
    public ProcessLogger(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Primary log method. Builds ProcessLog, serializes to JSON, and emits via SLF4J.
     */
    public void log(String service, ProcessStage stage, ProcessAction action, ProcessStatus status,
                    String source, String jobId, String recordId, String memberId,
                    String executionId, String correlationId, String traceId, String message) {
        log(service, stage, action, status, source, jobId, recordId, memberId,
                executionId, correlationId, traceId, message, 0);
    }

    /**
     * Log method with retryCount — for retry scenarios where the attempt number must be captured.
     */
    public void log(String service, ProcessStage stage, ProcessAction action, ProcessStatus status,
                    String source, String jobId, String recordId, String memberId,
                    String executionId, String correlationId, String traceId, String message,
                    int retryCount) {
        emit(ProcessLog.builder()
                .timestamp(Instant.now())
                .service(service)
                .stage(stage)
                .action(action)
                .status(status)
                .source(source)
                .jobId(jobId)
                .recordId(recordId)
                .memberId(memberId)
                .executionId(executionId)
                .correlationId(correlationId)
                .traceId(traceId)
                .message(message)
                .retryCount(retryCount)
                .build());
    }

    /**
     * Failure variant — includes error details and retryCount.
     */
    public void logFailure(String service, ProcessStage stage, ProcessAction action,
                           String source, String jobId, String recordId, String memberId,
                           String executionId, String correlationId, String traceId,
                           String message, String errorType, String errorMessage, int retryCount) {
        emit(ProcessLog.builder()
                .timestamp(Instant.now())
                .service(service)
                .stage(stage)
                .action(action)
                .status(ProcessStatus.FAILED)
                .source(source)
                .jobId(jobId)
                .recordId(recordId)
                .memberId(memberId)
                .executionId(executionId)
                .correlationId(correlationId)
                .traceId(traceId)
                .message(message)
                .errorType(errorType)
                .errorMessage(errorMessage)
                .retryCount(retryCount)
                .build());
    }

    /**
     * Validation failure variant — errorType is fixed to VALIDATION_ERROR.
     */
    public void logValidationFailure(String service, ProcessStage stage, ProcessAction action,
                                     String source, String jobId, String recordId, String memberId,
                                     String executionId, String correlationId, String traceId,
                                     String message, String errorMessage) {
        emit(ProcessLog.builder()
                .timestamp(Instant.now())
                .service(service)
                .stage(stage)
                .action(action)
                .status(ProcessStatus.VALIDATION_FAILED)
                .source(source)
                .jobId(jobId)
                .recordId(recordId)
                .memberId(memberId)
                .executionId(executionId)
                .correlationId(correlationId)
                .traceId(traceId)
                .message(message)
                .errorType("VALIDATION_ERROR")
                .errorMessage(errorMessage)
                .retryCount(0)
                .build());
    }

    private void emit(ProcessLog processLog) {
        try {
            log.info(mapper.writeValueAsString(processLog));
        } catch (Exception e) {
            log.error("ProcessLogger failed to serialize ProcessLog for service={} stage={} action={}",
                    processLog.getService(), processLog.getStage(), processLog.getAction(), e);
        }
    }
}
