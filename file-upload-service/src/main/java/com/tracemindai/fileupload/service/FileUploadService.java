package com.tracemindai.fileupload.service;

import com.tracemindai.common.event.RecordCreatedEvent;
import com.tracemindai.fileupload.config.Snowflake;
import com.tracemindai.fileupload.dto.FileUploadResponse;
import com.tracemindai.fileupload.dto.MemberRecord;
import com.tracemindai.fileupload.entity.Job;
import com.tracemindai.fileupload.entity.Record;
import com.tracemindai.fileupload.kafka.RecordProducer;
import com.tracemindai.fileupload.repository.JobRepository;
import com.tracemindai.fileupload.repository.RecordRepository;
import com.tracemindai.fileupload.util.CsvParser;
import com.tracemindai.fileupload.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {
    private final JobRepository jobRepository;
    private final RecordRepository recordRepository;
    private final CsvParser csvParser;
    private final RecordProducer recordProducer;

    private static final String JOB_STATUS_UPLOADED = "UPLOADED";
    private static final String RECORD_STATUS_RECEIVED = "RECEIVED";

    @Transactional
    public FileUploadResponse uploadCsv(MultipartFile file) {
        log.info("Starting CSV upload for file: {}", file.getOriginalFilename());

        List<MemberRecord> memberRecords = csvParser.parse(file);

        Job job = createJob(file.getOriginalFilename(), memberRecords.size());
        log.info("Created job with id: {} for {} records", job.getId(), memberRecords.size());

        createRecords(job.getJobId(), memberRecords);
        log.info("Created {} record entries in database", memberRecords.size());

        return FileUploadResponse.builder()
                .jobId(job.getJobId())
                .fileName(job.getFileName())
                .totalRecords(job.getTotalRecords())
                .status(job.getStatus())
                .build();
    }

    private Job createJob(String fileName, int totalRecords) {
        Job job = Job.builder()
                .fileName(fileName)
                .jobId(getJobId())
                .totalRecords(totalRecords)
                .status(JOB_STATUS_UPLOADED)
                .build();

        return jobRepository.save(job);
    }

    private void createRecords(String jobId, List<MemberRecord> memberRecords) {
        for (MemberRecord memberRecord : memberRecords) {
            String recordId = getRecordId();

            Record record = Record.builder()
                    .jobId(jobId)
                    .recordId(recordId)
                    .memberId(memberRecord.getMemberId())
                    .name(memberRecord.getName())
                    .mobile(memberRecord.getMobile())
                    .email(memberRecord.getEmail())
                    .communicationPreference(memberRecord.getCommunicationPreference())
                    .status(RECORD_STATUS_RECEIVED)
                    .build();

            recordRepository.save(record);

            publishRecordCreatedEvent(jobId, recordId, memberRecord);

            log.debug("Created and published event for record: {}", recordId);
        }
    }

    private void publishRecordCreatedEvent(String jobId, String recordId, MemberRecord memberRecord) {
        RecordCreatedEvent event = RecordCreatedEvent.builder()
                .jobId(jobId)
                .recordId(recordId)
                .memberId(memberRecord.getMemberId())
                .communicationPreference(memberRecord.getCommunicationPreference())
                .correlationId(jobId)
                .traceId(recordId)
                .email(memberRecord.getEmail())
                .mobile(memberRecord.getMobile())
                .build();

        recordProducer.publish(event);
    }

    private String getJobId() {
        return "JOB-" + IdGenerator.generateSnowflakeId();
    }

    private String getRecordId() {
        return "REC-" + IdGenerator.generateSnowflakeId();
    }
}
