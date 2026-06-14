package com.tracemindai.email.kafka;

import com.tracemindai.common.event.EmailRequestEvent;
import com.tracemindai.email.exception.ValidationException;
import com.tracemindai.email.service.DltEventService;
import com.tracemindai.email.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailRequestEventListener {
    private final EmailService emailService;
    private final DltEventService dltEventService;

    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000),
        autoCreateTopics = "true",
        exclude = ValidationException.class
    )
    @KafkaListener(topics = "email-request", groupId = "email-service-group")
    public void onEmailRequest(EmailRequestEvent event) {
        log.info("Received event {}", event.getRecordId());
        emailService.processEmailRequest(event);
    }

    @DltHandler
    public void dlt(EmailRequestEvent event, Exception ex) {
        log.error("Message moved to DLT. recordId={}, error={}", event.getRecordId(), ex.getMessage());
        dltEventService.save("email-service", event, ex);
    }
}
