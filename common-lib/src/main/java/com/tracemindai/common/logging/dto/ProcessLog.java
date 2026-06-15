package com.tracemindai.common.logging.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tracemindai.common.logging.enums.ProcessAction;
import com.tracemindai.common.logging.enums.ProcessStage;
import com.tracemindai.common.logging.enums.ProcessStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProcessLog {
    Instant timestamp;
    String service;
    ProcessStage stage;
    ProcessAction action;
    ProcessStatus status;
    String source;
    String jobId;
    String recordId;
    String memberId;
    String executionId;
    String correlationId;
    String traceId;
    String message;
    String errorType;
    String errorMessage;
    Integer retryCount;
}
