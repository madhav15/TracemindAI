package com.tracemindai.print.kafka;

import com.tracemindai.common.event.PrintRequestEvent;
import com.tracemindai.common.logging.ProcessLogger;
import com.tracemindai.common.logging.enums.ProcessAction;
import com.tracemindai.common.logging.enums.ProcessStage;
import com.tracemindai.common.logging.enums.ProcessStatus;
import com.tracemindai.print.service.DltEventService;
import com.tracemindai.print.service.PrintService;
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
public class PrintRequestEventListener {

    private static final String SERVICE_NAME = "print-service";
    private static final String SOURCE_KAFKA = "topic:print-request";

    private final PrintService printService;
    private final DltEventService dltEventService;
    private final ProcessLogger processLogger;

    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000),
        autoCreateTopics = "true"
    )
    @KafkaListener(topics = "print-request", groupId = "print-service-group")
    public void onPrintRequest(
            @Payload PrintRequestEvent event,
            @Header(name = KafkaHeaders.DELIVERY_ATTEMPT, required = false) Integer deliveryAttempt) {

        int attempt = deliveryAttempt != null ? deliveryAttempt : 1;

        if (attempt > 1) {
            processLogger.log(SERVICE_NAME, ProcessStage.PRINT, ProcessAction.RETRY,
                    ProcessStatus.RETRYING, SOURCE_KAFKA,
                    event.getJobId(), event.getRecordId(), event.getMemberId(),
                    null, event.getCorrelationId(), event.getTraceId(),
                    "Retry attempt " + attempt + " of 3 initiated after processing error",
                    attempt - 1);
        }

        log.info("Received PrintRequestEvent for recordId: {} from jobId: {}",
            event.getRecordId(), event.getJobId());

        printService.processPrintRequest(event);
    }

    @DltHandler
    public void dlt(PrintRequestEvent event, Exception ex) {
        log.error("Message moved to DLT. recordId={}, error={}", event.getRecordId(), ex.getMessage());

        processLogger.logFailure(SERVICE_NAME, ProcessStage.DLT, ProcessAction.DLT_PUBLISHED,
                SOURCE_KAFKA,
                event.getJobId(), event.getRecordId(), event.getMemberId(),
                null, event.getCorrelationId(), event.getTraceId(),
                "All retry attempts exhausted, message routed to dead letter topic",
                ex.getClass().getSimpleName(), ex.getMessage(), 3);

        dltEventService.save("print-service", event, ex);
    }
}
