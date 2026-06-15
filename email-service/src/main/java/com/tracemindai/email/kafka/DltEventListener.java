package com.tracemindai.email.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracemindai.common.event.EmailRequestEvent;
import com.tracemindai.common.logging.ProcessLogger;
import com.tracemindai.common.logging.enums.ProcessAction;
import com.tracemindai.common.logging.enums.ProcessStage;
import com.tracemindai.common.logging.enums.ProcessStatus;
import com.tracemindai.email.entity.DltEvent;
import com.tracemindai.email.repository.DltEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DltEventListener {

    private static final String SERVICE_NAME = "email-service";

    private final DltEventRepository dltEventRepository;
    private final ObjectMapper objectMapper;
    private final ProcessLogger processLogger;

    @DltHandler
    public void handleDltEvent(
            @Payload EmailRequestEvent event,
            @Header(name = "kafka_receivedTopic") String topic,
            @Header(name = "kafka_exception_message", required = false) String exceptionMessage) {

        log.debug("Processing DLT message from topic: {}, recordId: {}, error: {}",
                topic, event.getRecordId(), exceptionMessage);

        String errorMessage = exceptionMessage != null ? exceptionMessage : "Unknown error";

        // 9. RETRY_EXHAUSTED
        processLogger.logFailure(SERVICE_NAME, ProcessStage.DLT, ProcessAction.DLT_PUBLISHED,
                "topic:" + topic,
                event.getJobId(), event.getRecordId(), event.getMemberId(),
                null, event.getCorrelationId(), event.getTraceId(),
                "All retry attempts exhausted, message routed to dead letter topic",
                "DLT_RECEIVED", errorMessage, 3);

        String payload = serializePayload(event);

        DltEvent dltEvent = DltEvent.builder()
                .serviceName(SERVICE_NAME)
                .errorMessage(errorMessage)
                .payload(payload)
                .build();

        dltEventRepository.save(dltEvent);
        log.debug("Saved DLT event for recordId: {} to dlt_events table", event.getRecordId());
    }

    private String serializePayload(EmailRequestEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            log.warn("Failed to serialize event payload", e);
            return event.toString();
        }
    }
}