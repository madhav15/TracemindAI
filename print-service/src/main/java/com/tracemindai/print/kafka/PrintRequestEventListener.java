package com.tracemindai.print.kafka;

import com.tracemindai.common.event.PrintRequestEvent;
import com.tracemindai.print.service.DltEventService;
import com.tracemindai.print.service.PrintService;
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
public class PrintRequestEventListener {
    private final PrintService printService;
    private final DltEventService dltEventService;

    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000),
        autoCreateTopics = "true"
    )
    @KafkaListener(topics = "print-request", groupId = "print-service-group")
    public void onPrintRequest(PrintRequestEvent event) {
        log.info("Received PrintRequestEvent for recordId: {} from jobId: {}",
            event.getRecordId(), event.getJobId());

        printService.processPrintRequest(event);
    }

    @DltHandler
    public void dlt(PrintRequestEvent event, Exception ex) {
        log.error("Message moved to DLT. recordId={}, error={}", event.getRecordId(), ex.getMessage());
        dltEventService.save("print-service", event, ex);
    }
}
