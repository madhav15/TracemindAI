package com.tracemindai.archival.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracemindai.common.event.ArchivalRequestEvent;
import com.tracemindai.archival.entity.DltEvent;
import com.tracemindai.archival.repository.DltEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class DltEventService {
    private final DltEventRepository dltEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void save(String serviceName, ArchivalRequestEvent event, Exception ex) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            DltEvent dltEvent = new DltEvent();
            dltEvent.setServiceName(serviceName);
            dltEvent.setErrorMessage(ex.getMessage() != null ? ex.getMessage() : ex.getClass().getName());
            dltEvent.setPayload(payload);
            dltEvent.setCreatedAt(LocalDateTime.now());

            log.info("Saving DLT event to database for jobId {} and recordId {}",
                event.getJobId(), event.getRecordId());
            dltEventRepository.save(dltEvent);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize DLT event", e);
            throw new RuntimeException("Failed to save DLT event", e);
        }
    }
}
