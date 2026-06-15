package com.tracemindai.email.kafka;

import com.tracemindai.common.event.EmailRequestEvent;
import com.tracemindai.common.logging.ProcessLogger;
import com.tracemindai.common.logging.enums.ProcessAction;
import com.tracemindai.common.logging.enums.ProcessStage;
import com.tracemindai.common.logging.enums.ProcessStatus;
import com.tracemindai.email.exception.ValidationException;
import com.tracemindai.email.service.DltEventService;
import com.tracemindai.email.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailRequestEventListener {

    private static final String SERVICE_NAME = "email-service";
    private static final String SOURCE_KAFKA = "topic:email-request";

    private final EmailService emailService;
    private final DltEventService dltEventService;
    private final ProcessLogger processLogger;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000),
            autoCreateTopics = "true",
            exclude = ValidationException.class
    )
    @KafkaListener(topics = "email-request", groupId = "email-service-group")
    public void onEmailRequest(
            @Payload EmailRequestEvent event,
            @Header(name = KafkaHeaders.DELIVERY_ATTEMPT, required = false) Integer deliveryAttempt) {

        int attempt = deliveryAttempt != null ? deliveryAttempt : 1;

        if (attempt > 1) {
            // 8. RETRY_STARTED — fires on each retry attempt
            processLogger.log(SERVICE_NAME, ProcessStage.EMAIL, ProcessAction.RETRY,
                    ProcessStatus.RETRYING, SOURCE_KAFKA,
                    event.getJobId(), event.getRecordId(), event.getMemberId(),
                    null, event.getCorrelationId(), event.getTraceId(),
                    "Retry attempt " + attempt + " of 3 initiated after processing error",
                    attempt - 1);
        }

        log.debug("Received event {}, deliveryAttempt={}", event.getRecordId(), attempt);
        emailService.processEmailRequest(event);
    }

    @DltHandler
    public void dlt(EmailRequestEvent event, Exception ex) {
        log.error("Message moved to DLT. recordId={}, error={}", event.getRecordId(), ex.getMessage());

        // 9. RETRY_EXHAUSTED
        processLogger.logFailure(SERVICE_NAME, ProcessStage.DLT, ProcessAction.DLT_PUBLISHED,
                SOURCE_KAFKA,
                event.getJobId(), event.getRecordId(), event.getMemberId(),
                null, event.getCorrelationId(), event.getTraceId(),
                "All retry attempts exhausted, message routed to dead letter topic",
                ex.getClass().getSimpleName(), ex.getMessage(), 3);

        dltEventService.save("email-service", event, ex);
    }
}