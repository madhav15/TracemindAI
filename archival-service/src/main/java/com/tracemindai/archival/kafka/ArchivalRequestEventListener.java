package com.tracemindai.archival.kafka;

import com.tracemindai.common.event.ArchivalRequestEvent;
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
    private final ArchivalService archivalService;
    private final DltEventService dltEventService;

    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000),
        autoCreateTopics = "true",
        topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
        dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(topics = "archival-request", groupId = "archival-service-group")
    public void onArchivalRequest(ArchivalRequestEvent event) {
        log.info("Received ArchivalRequestEvent for recordId: {} from jobId: {}",
            event.getRecordId(), event.getJobId());

        archivalService.processArchivalRequest(event);
    }

    @DltHandler
    public void dlt(ConsumerRecord<String, ArchivalRequestEvent> record) {
        log.error("Message moved to DLT. partition={}, offset={}, recordId={}",
            record.partition(), record.offset(), record.value().getRecordId());
        dltEventService.save("archival-service", record.value(),
            new Exception("Message failed after max retries"));
    }
}
