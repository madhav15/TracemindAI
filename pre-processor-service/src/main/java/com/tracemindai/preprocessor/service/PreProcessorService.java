package com.tracemindai.preprocessor.service;

import com.tracemindai.common.event.EmailRequestEvent;
import com.tracemindai.common.event.PrintRequestEvent;
import com.tracemindai.common.event.RecordCreatedEvent;
import com.tracemindai.common.logging.ProcessLogger;
import com.tracemindai.common.logging.enums.ProcessAction;
import com.tracemindai.common.logging.enums.ProcessStage;
import com.tracemindai.common.logging.enums.ProcessStatus;
import com.tracemindai.preprocessor.entity.PreProcessorTracking;
import com.tracemindai.preprocessor.kafka.RequestEventProducer;
import com.tracemindai.preprocessor.repository.PreProcessorTrackingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PreProcessorService {

    private static final String SERVICE_NAME = "pre-processor-service";
    private static final String SOURCE_KAFKA = "topic:record-created";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_ROUTED = "ROUTED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String PREF_EMAIL = "E";
    private static final String PREF_PRINT = "P";
    private static final String TYPE_EMAIL = "EMAIL";
    private static final String TYPE_PRINT = "PRINT";

    private final PreProcessorTrackingRepository repository;
    private final RequestEventProducer eventProducer;
    private final ProcessLogger processLogger;

    @Transactional
    public void processRecord(RecordCreatedEvent event) {
        log.debug("Processing record: {} from job: {}", event.getRecordId(), event.getJobId());

        processLogger.log(SERVICE_NAME, ProcessStage.PRE_PROCESSOR, ProcessAction.EVENT_CONSUMED,
                ProcessStatus.STARTED, SOURCE_KAFKA,
                event.getJobId(), event.getRecordId(), event.getMemberId(), null,
                event.getCorrelationId(), event.getTraceId(),
                "Record received for pre-processing");

        PreProcessorTracking tracking = PreProcessorTracking.builder()
                .jobId(event.getJobId())
                .recordId(event.getRecordId())
                .memberId(event.getMemberId())
                .status(STATUS_PROCESSING)
                .processingType(event.getCommunicationPreference())
                .build();

        repository.save(tracking);
        log.debug("Saved tracking record for recordId: {}", event.getRecordId());

        routeByPreference(event);
    }

    private void routeByPreference(RecordCreatedEvent event) {
        String recordId = event.getRecordId();
        String communicationPreference = event.getCommunicationPreference();

        if (PREF_EMAIL.equals(communicationPreference)) {
            publishEmailRequest(event);
            updateTrackingAsRouted(recordId, TYPE_EMAIL);
            log.info("Routed record {} to EMAIL pipeline", recordId);
        } else if (PREF_PRINT.equals(communicationPreference)) {
            publishPrintRequest(event);
            updateTrackingAsRouted(recordId, TYPE_PRINT);
            log.info("Routed record {} to PRINT pipeline", recordId);
        } else {
            updateTrackingAsFailed(recordId);
            log.warn("Unknown communication preference: {} for record: {}",
                    communicationPreference, recordId);
        }
    }

    private void publishEmailRequest(RecordCreatedEvent event) {
        EmailRequestEvent emailRequest = EmailRequestEvent.builder()
                .jobId(event.getJobId())
                .recordId(event.getRecordId())
                .memberId(event.getMemberId())
                .correlationId(event.getCorrelationId())
                .traceId(event.getTraceId())
                .mobile(event.getMobile())
                .email(event.getEmail())
                .build();

        eventProducer.publishEmailRequest(emailRequest);

        processLogger.log(SERVICE_NAME, ProcessStage.PRE_PROCESSOR, ProcessAction.EVENT_PUBLISHED,
                ProcessStatus.SUCCESS, "topic:email-request",
                event.getJobId(), event.getRecordId(), event.getMemberId(), null,
                event.getCorrelationId(), event.getTraceId(),
                "Record routed to email pipeline");
    }

    private void publishPrintRequest(RecordCreatedEvent event) {
        PrintRequestEvent printRequest = PrintRequestEvent.builder()
                .jobId(event.getJobId())
                .recordId(event.getRecordId())
                .memberId(event.getMemberId())
                .mobile(event.getMobile())
                .email(event.getEmail())
                .correlationId(event.getCorrelationId())
                .traceId(event.getTraceId())
                .build();

        eventProducer.publishPrintRequest(printRequest);

        processLogger.log(SERVICE_NAME, ProcessStage.PRE_PROCESSOR, ProcessAction.EVENT_PUBLISHED,
                ProcessStatus.SUCCESS, "topic:print-request",
                event.getJobId(), event.getRecordId(), event.getMemberId(), null,
                event.getCorrelationId(), event.getTraceId(),
                "Record routed to print pipeline");
    }

    private void updateTrackingAsRouted(String recordId, String processingType) {
        PreProcessorTracking tracking = repository.findByRecordId(recordId)
                .orElseThrow(() -> new IllegalStateException(
                        "Tracking record not found for recordId: " + recordId));

        tracking.setStatus(STATUS_ROUTED);
        tracking.setProcessingType(processingType);
        repository.save(tracking);

        log.debug("Updated tracking record for recordId: {} with status: {} and processingType: {}",
                recordId, STATUS_ROUTED, processingType);
    }

    private void updateTrackingAsFailed(String recordId) {
        PreProcessorTracking tracking = repository.findByRecordId(recordId)
                .orElseThrow(() -> new IllegalStateException(
                        "Tracking record not found for recordId: " + recordId));

        tracking.setStatus(STATUS_FAILED);
        repository.save(tracking);

        log.debug("Updated tracking record for recordId: {} with status: {}",
                recordId, STATUS_FAILED);
    }
}
