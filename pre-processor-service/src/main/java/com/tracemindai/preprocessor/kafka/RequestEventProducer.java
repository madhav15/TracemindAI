package com.tracemindai.preprocessor.kafka;

import com.tracemindai.common.event.EmailRequestEvent;
import com.tracemindai.common.event.PrintRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestEventProducer {
    private static final String TOPIC_EMAIL_REQUEST = "email-request";
    private static final String TOPIC_PRINT_REQUEST = "print-request";

    private final KafkaTemplate<String, EmailRequestEvent> emailKafkaTemplate;
    private final KafkaTemplate<String, PrintRequestEvent> printKafkaTemplate;

    public void publishEmailRequest(EmailRequestEvent event) {
        try {
            emailKafkaTemplate.send(TOPIC_EMAIL_REQUEST, event.getRecordId(), event);
            log.debug("Published EmailRequestEvent for recordId: {} to topic: {}",
                event.getRecordId(), TOPIC_EMAIL_REQUEST);
        } catch (Exception e) {
            log.error("Failed to publish EmailRequestEvent for recordId: {}",
                event.getRecordId(), e);
            throw new KafkaPublishException("Failed to publish email request event to Kafka", e);
        }
    }

    public void publishPrintRequest(PrintRequestEvent event) {
        try {
            printKafkaTemplate.send(TOPIC_PRINT_REQUEST, event.getRecordId(), event);
            log.debug("Published PrintRequestEvent for recordId: {} to topic: {}",
                event.getRecordId(), TOPIC_PRINT_REQUEST);
        } catch (Exception e) {
            log.error("Failed to publish PrintRequestEvent for recordId: {}",
                event.getRecordId(), e);
            throw new KafkaPublishException("Failed to publish print request event to Kafka", e);
        }
    }
}
