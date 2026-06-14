package com.tracemindai.print.service;

import com.tracemindai.common.event.ArchivalRequestEvent;
import com.tracemindai.common.event.PrintRequestEvent;
import com.tracemindai.print.entity.PrintJob;
import com.tracemindai.print.repository.PrintJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrintService {
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_COMPLETED = "COMPLETED";

    private final PrintJobRepository repository;
    private final KafkaTemplate<String, ArchivalRequestEvent> kafkaTemplate;

    @Transactional
    public void processPrintRequest(PrintRequestEvent event) {
        log.info("[jobId={}][recordId={}][memberId={}] Creating print job",
            event.getJobId(), event.getRecordId(), event.getMemberId());

        PrintJob job = PrintJob.builder()
            .jobId(event.getJobId())
            .recordId(event.getRecordId())
            .memberId(event.getMemberId())
            .status(STATUS_PROCESSING)
            .build();

        repository.save(job);
        log.debug("Saved print job for recordId: {}", event.getRecordId());

        log.info("[jobId={}][recordId={}][memberId={}] Printing document",
            event.getJobId(), event.getRecordId(), event.getMemberId());

        job.setStatus(STATUS_COMPLETED);
        repository.save(job);
        log.debug("Marked print job as COMPLETED for recordId: {}", event.getRecordId());

        publishArchivalRequest(event);
    }

    private void publishArchivalRequest(PrintRequestEvent event) {
        ArchivalRequestEvent archivalEvent = ArchivalRequestEvent.builder()
            .jobId(event.getJobId())
            .recordId(event.getRecordId())
            .memberId(event.getMemberId())
            .correlationId(event.getCorrelationId())
            .traceId(event.getTraceId())
            .build();

        kafkaTemplate.send("archival-request", event.getRecordId(), archivalEvent);
        log.info("Published archival request for recordId={}", event.getRecordId());
    }
}
