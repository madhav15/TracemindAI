package com.tracemindai.preprocessor.kafka;

import com.tracemindai.common.event.RecordCreatedEvent;
import com.tracemindai.common.logging.ProcessLogger;
import com.tracemindai.common.logging.enums.ProcessAction;
import com.tracemindai.common.logging.enums.ProcessStage;
import com.tracemindai.common.logging.enums.ProcessStatus;
import com.tracemindai.preprocessor.service.PreProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecordCreatedEventListener {

    private static final String SERVICE_NAME = "pre-processor-service";

    private final PreProcessorService preProcessorService;
    private final ProcessLogger processLogger;

    @KafkaListener(
        topics = "record-created",
        groupId = "pre-processor-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onRecordCreated(RecordCreatedEvent event) {
        log.debug("Received RecordCreatedEvent for recordId: {} from jobId: {}",
            event.getRecordId(), event.getJobId());

        processLogger.log(SERVICE_NAME, ProcessStage.PRE_PROCESSOR, ProcessAction.EVENT_CONSUMED,
                ProcessStatus.STARTED, "topic:record-created",
                event.getJobId(), event.getRecordId(), event.getMemberId(), null,
                event.getCorrelationId(), event.getTraceId(),
                "RecordCreatedEvent received for pre-processing");

        preProcessorService.processRecord(event);
    }
}
