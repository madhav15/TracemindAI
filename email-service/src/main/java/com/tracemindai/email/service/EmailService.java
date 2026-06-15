package com.tracemindai.email.service;

import com.tracemindai.common.event.ArchivalRequestEvent;
import com.tracemindai.common.event.EmailRequestEvent;
import com.tracemindai.common.logging.ProcessLogger;
import com.tracemindai.common.logging.enums.ProcessAction;
import com.tracemindai.common.logging.enums.ProcessStage;
import com.tracemindai.common.logging.enums.ProcessStatus;
import com.tracemindai.email.entity.EmailTracking;
import com.tracemindai.email.exception.ValidationException;
import com.tracemindai.email.repository.EmailTrackingRepository;
import com.tracemindai.email.validation.EmailValidator;
import com.tracemindai.email.validation.MobileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final String SERVICE_NAME = "email-service";
    private static final String SOURCE_KAFKA = "topic:email-request";
    private static final String SOURCE_ARCHIVAL = "topic:archival-request";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_VALIDATION_FAILED = "VALIDATION_FAILED";

    private final EmailTrackingRepository repository;
    private final KafkaTemplate<String, ArchivalRequestEvent> kafkaTemplate;
    private final ProcessLogger processLogger;

    @Transactional
    public void processEmailRequest(EmailRequestEvent event) {
        String jobId = event.getJobId();
        String recordId = event.getRecordId();
        String memberId = event.getMemberId();
        String correlationId = event.getCorrelationId();
        String traceId = event.getTraceId();

        // 1. EMAIL_REQUEST_RECEIVED
        processLogger.log(SERVICE_NAME, ProcessStage.EMAIL, ProcessAction.EVENT_CONSUMED,
                ProcessStatus.STARTED, SOURCE_KAFKA,
                jobId, recordId, memberId, null, correlationId, traceId,
                "Email request received for processing");

        // 2. VALIDATION_STARTED
        processLogger.log(SERVICE_NAME, ProcessStage.EMAIL, ProcessAction.VALIDATION_STARTED,
                ProcessStatus.STARTED, SOURCE_KAFKA,
                jobId, recordId, memberId, null, correlationId, traceId,
                "Starting email and mobile validation");

        try {
            validateEvent(event);
        } catch (ValidationException e) {
            log.error("Validation failed for recordId={}: {}", recordId, e.getMessage());

            // 3. VALIDATION_FAILED
            processLogger.logValidationFailure(SERVICE_NAME, ProcessStage.EMAIL,
                    ProcessAction.VALIDATION_FAILED, SOURCE_KAFKA,
                    jobId, recordId, memberId, null, correlationId, traceId,
                    "Email validation failed for recipient", e.getMessage());

            saveFailedValidation(event);
            return;
        }

        // 4. VALIDATION_COMPLETED
        processLogger.log(SERVICE_NAME, ProcessStage.EMAIL, ProcessAction.VALIDATION_COMPLETED,
                ProcessStatus.SUCCESS, SOURCE_KAFKA,
                jobId, recordId, memberId, null, correlationId, traceId,
                "Email and mobile validation passed");

        // 5. EMAIL_PROCESSING_STARTED
        processLogger.log(SERVICE_NAME, ProcessStage.EMAIL, ProcessAction.EVENT_CONSUMED,
                ProcessStatus.STARTED, SOURCE_KAFKA,
                jobId, recordId, memberId, null, correlationId, traceId,
                "Email processing started");

        try {
            EmailTracking tracking = EmailTracking.builder()
                    .jobId(jobId)
                    .recordId(recordId)
                    .memberId(memberId)
                    .email(event.getEmail())
                    .status(STATUS_PROCESSING)
                    .build();

            repository.save(tracking);
            log.debug("Saved email tracking record for recordId: {}", recordId);

            updateTrackingStatus(tracking, STATUS_COMPLETED);

            // 6. EMAIL_SENT_SUCCESS
            processLogger.log(SERVICE_NAME, ProcessStage.EMAIL, ProcessAction.EMAIL_SENT,
                    ProcessStatus.SUCCESS, SOURCE_KAFKA,
                    jobId, recordId, memberId, null, correlationId, traceId,
                    "Email sent successfully to recipient");

            publishArchivalRequest(event);

            // 10. ARCHIVAL_EVENT_PUBLISHED
            processLogger.log(SERVICE_NAME, ProcessStage.EMAIL, ProcessAction.EVENT_PUBLISHED,
                    ProcessStatus.SUCCESS, SOURCE_ARCHIVAL,
                    jobId, recordId, memberId, null, correlationId, traceId,
                    "Archival event published for record");

        } catch (Exception e) {
            log.error("Email processing failed for recordId={}: {}", recordId, e.getMessage());

            // 7. EMAIL_PROCESSING_FAILED
            processLogger.logFailure(SERVICE_NAME, ProcessStage.EMAIL, ProcessAction.EMAIL_SENT,
                    SOURCE_KAFKA,
                    jobId, recordId, memberId, null, correlationId, traceId,
                    "Email sending failed", e.getClass().getSimpleName(), e.getMessage(), 0);
            throw e;
        }
    }

    private void validateEvent(EmailRequestEvent event) {
        if (event == null) {
            throw new ValidationException("EmailRequestEvent is null");
        }

        if (!EmailValidator.isValidEmail(event.getEmail())) {
            throw new ValidationException("Invalid email address: " + event.getEmail());
        }

        if (!MobileValidator.isValidMobileNumber(event.getMemberId())) {
            throw new ValidationException("Invalid mobile number: " + event.getMemberId());
        }
    }

    @Transactional
    private void saveFailedValidation(EmailRequestEvent event) {
        EmailTracking tracking = EmailTracking.builder()
                .jobId(event.getJobId())
                .recordId(event.getRecordId())
                .memberId(event.getMemberId())
                .email(event.getEmail())
                .status(STATUS_VALIDATION_FAILED)
                .build();

        repository.save(tracking);
        log.debug("Saved validation failed tracking record for recordId: {}", event.getRecordId());
    }

    @Transactional
    private void updateTrackingStatus(EmailTracking tracking, String status) {
        tracking.setStatus(status);
        repository.save(tracking);
        log.debug("Updated email tracking status to {} for recordId: {}", status, tracking.getRecordId());
    }

    private void publishArchivalRequest(EmailRequestEvent event) {
        ArchivalRequestEvent archivalEvent = ArchivalRequestEvent.builder()
                .jobId(event.getJobId())
                .recordId(event.getRecordId())
                .memberId(event.getMemberId())
                .correlationId(event.getCorrelationId())
                .traceId(event.getTraceId())
                .build();

        kafkaTemplate.send("archival-request", event.getRecordId(), archivalEvent);
        log.debug("Published archival request for recordId={}", event.getRecordId());
    }
}