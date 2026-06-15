package com.tracemindai.archival.kafka;

import com.tracemindai.common.event.ArchivalRequestEvent;
import com.tracemindai.common.logging.ProcessLogger;
import com.tracemindai.common.logging.enums.ProcessAction;
import com.tracemindai.common.logging.enums.ProcessStage;
import com.tracemindai.common.logging.enums.ProcessStatus;
import com.tracemindai.archival.service.ArchivalService;
import com.tracemindai.archival.service.DltEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArchivalRequestEventListener {

    private static final String SERVICE_NAME = "archival-service";
    private static final String SOURCE_KAFKA = "topic:archival-request";

    private final ArchivalService archivalService;
    private final DltEventService dltEventService;
    private final ProcessLogger processLogger;

    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000),
        autoCreateTopics = "true",
        topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
        dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(topics = "archival-request", groupId = "archival-service-group")
    public void onArchivalRequest(ArchivalRequestEvent event) {
        processLogger.log(SERVICE_NAME, ProcessStage.ARCHIVAL, ProcessAction.EVENT_CONSUMED,
                ProcessStatus.STARTED, SOURCE_KAFKA,
                event.getJobId(), event.getRecordId(), event.getMemberId(), null,
                event.getCorrelationId(), event.getTraceId(),
                "ArchivalRequestEvent received for processing");

        log.info("Received ArchivalRequestEvent for recordId: {} from jobId: {}",
            event.getRecordId(), event.getJobId());

        archivalService.processArchivalRequest(event);
    }

    @DltHandler
    public void dlt(ConsumerRecord<String, ArchivalRequestEvent> record) {
        ArchivalRequestEvent event = record.value();
        log.error("Message moved to DLT. partition={}, offset={}, recordId={}",
            record.partition(), record.offset(), event.getRecordId());

        processLogger.logFailure(SERVICE_NAME, ProcessStage.DLT, ProcessAction.DLT_PUBLISHED,
                "topic:" + record.topic(),
                event.getJobId(), event.getRecordId(), event.getMemberId(),
                null, event.getCorrelationId(), event.getTraceId(),
                "All retry attempts exhausted, message routed to dead letter topic",
                "DLT_RECEIVED", "Message failed after max retries", 3);

        dltEventService.save("archival-service", event,
            new Exception("Message failed after max retries"));
    }
}
