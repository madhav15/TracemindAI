package com.tracemindai.preprocessor.kafka;

import com.tracemindai.common.event.RecordCreatedEvent;
import com.tracemindai.preprocessor.service.PreProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecordCreatedEventListener {
    private final PreProcessorService preProcessorService;

    @KafkaListener(
        topics = "record-created",
        groupId = "pre-processor-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onRecordCreated(RecordCreatedEvent event) {
        log.debug("Received RecordCreatedEvent for recordId: {} from jobId: {}",
            event.getRecordId(), event.getJobId());

        preProcessorService.processRecord(event);
    }
}
