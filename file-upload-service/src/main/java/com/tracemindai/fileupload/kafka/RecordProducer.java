package com.tracemindai.fileupload.kafka;

import com.tracemindai.common.constant.KafkaTopics;
import com.tracemindai.common.event.RecordCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import static com.tracemindai.common.constant.KafkaTopics.RECORD_CREATED;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecordProducer {


    private final KafkaTemplate<String, RecordCreatedEvent> kafkaTemplate;

    public void publish(RecordCreatedEvent event) {
        try {
            kafkaTemplate.send(RECORD_CREATED, event.getRecordId(), event);
            log.info("Published RecordCreatedEvent for recordId: {} to topic: {}",
                event.getRecordId(), RECORD_CREATED);
        } catch (Exception e) {
            log.error("Failed to publish RecordCreatedEvent for recordId: {}",
                event.getRecordId(), e);
            throw new KafkaPublishException("Failed to publish event to Kafka", e);
        }
    }
}
