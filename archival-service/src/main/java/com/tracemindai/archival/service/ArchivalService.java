package com.tracemindai.archival.service;

import com.tracemindai.common.event.ArchivalRequestEvent;
import com.tracemindai.common.logging.ProcessLogger;
import com.tracemindai.common.logging.enums.ProcessAction;
import com.tracemindai.common.logging.enums.ProcessStage;
import com.tracemindai.common.logging.enums.ProcessStatus;
import com.tracemindai.archival.entity.Archive;
import com.tracemindai.archival.repository.ArchiveRepository;
import com.tracemindai.archival.repository.JobRepository;
import com.tracemindai.archival.repository.RecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArchivalService {

    private static final String SERVICE_NAME = "archival-service";
    private static final String SOURCE_KAFKA = "topic:archival-request";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_ARCHIVAL_COMPLETED = "ARCHIVAL_COMPLETED";

    private final ArchiveRepository archiveRepository;
    private final RecordRepository recordRepository;
    private final JobRepository jobRepository;
    private final ProcessLogger processLogger;

    @Transactional
    public void processArchivalRequest(ArchivalRequestEvent event) {
        processLogger.log(SERVICE_NAME, ProcessStage.ARCHIVAL, ProcessAction.EVENT_CONSUMED,
                ProcessStatus.STARTED, SOURCE_KAFKA,
                event.getJobId(), event.getRecordId(), event.getMemberId(), null,
                event.getCorrelationId(), event.getTraceId(),
                "Archival request received for processing");

        log.info("[jobId={}][recordId={}][memberId={}] Starting archival processing",
            event.getJobId(), event.getRecordId(), event.getMemberId());

        Archive archivalJob = Archive.builder()
            .jobId(event.getJobId())
            .recordId(event.getRecordId())
            .memberId(event.getMemberId())
            .status(STATUS_PROCESSING)
            .build();

        archiveRepository.save(archivalJob);

        processLogger.log(SERVICE_NAME, ProcessStage.ARCHIVAL, ProcessAction.ARCHIVAL_STARTED,
                ProcessStatus.STARTED, SOURCE_KAFKA,
                event.getJobId(), event.getRecordId(), event.getMemberId(), null,
                event.getCorrelationId(), event.getTraceId(),
                "Archival started for record");

        log.debug("Saved archival job for recordId: {}", event.getRecordId());

        int i = recordRepository.updateStatusByRecordId(event.getRecordId(), STATUS_ARCHIVAL_COMPLETED);

        log.debug("Updated record status to ARCHIVAL_COMPLETED for recordId: {}, count = {}", event.getRecordId(), i);

        long nonArchivedCount = jobRepository.countNonArchivedRecords(event.getJobId(), STATUS_ARCHIVAL_COMPLETED);
        log.debug("Non-archived record count for jobId {}: {}", event.getJobId(), nonArchivedCount);

        if (nonArchivedCount == 0) {
            jobRepository.updateStatusByJobId(event.getJobId(), STATUS_ARCHIVAL_COMPLETED);
            log.info("[jobId={}] All records archived, updating job status to ARCHIVAL_COMPLETED",
                event.getJobId());
        }

        archivalJob.setStatus(STATUS_COMPLETED);
        archiveRepository.save(archivalJob);

        processLogger.log(SERVICE_NAME, ProcessStage.ARCHIVAL, ProcessAction.ARCHIVAL_COMPLETED,
                ProcessStatus.SUCCESS, SOURCE_KAFKA,
                event.getJobId(), event.getRecordId(), event.getMemberId(), null,
                event.getCorrelationId(), event.getTraceId(),
                "Archival completed for record");

        log.debug("Marked archival job as COMPLETED for recordId: {}", event.getRecordId());

        log.info("[jobId={}][recordId={}][memberId={}] Archival processing completed",
            event.getJobId(), event.getRecordId(), event.getMemberId());
    }
}
