package com.tracemindai.email.service;

import com.tracemindai.common.event.ArchivalRequestEvent;
import com.tracemindai.common.event.EmailRequestEvent;
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
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_VALIDATION_FAILED = "VALIDATION_FAILED";

    private final EmailTrackingRepository repository;
    private final KafkaTemplate<String, ArchivalRequestEvent> kafkaTemplate;

    @Transactional
    public void processEmailRequest(EmailRequestEvent event) {
        try {
            validateEvent(event);
            log.info("[jobId={}][recordId={}][memberId={}] Processing email for {}",
                event.getJobId(), event.getRecordId(), event.getMemberId(), event.getEmail());

            EmailTracking tracking = EmailTracking.builder()
                .jobId(event.getJobId())
                .recordId(event.getRecordId())
                .memberId(event.getMemberId())
                .email(event.getEmail())
                .status(STATUS_PROCESSING)
                .build();

            repository.save(tracking);
            log.info("Saved email tracking record for recordId: {}", event.getRecordId());

            updateTrackingStatus(tracking, STATUS_COMPLETED);
            publishArchivalRequest(event);
        } catch (ValidationException e) {
            log.error("[jobId={}][recordId={}][memberId={}] Validation failed: {}",
                event.getJobId(), event.getRecordId(), event.getMemberId(), e.getMessage());
            saveFailedValidation(event);
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
        log.info("Saved validation failed tracking record for recordId: {}", event.getRecordId());
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
        log.info("Published archival request for recordId={}", event.getRecordId());
    }
}
