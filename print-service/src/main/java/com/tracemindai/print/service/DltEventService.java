package com.tracemindai.print.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracemindai.common.event.PrintRequestEvent;
import com.tracemindai.print.entity.DltEvent;
import com.tracemindai.print.repository.DltEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class DltEventService {
    private final DltEventRepository dltEventRepository;

    public void save(String serviceName, PrintRequestEvent event, Exception ex) {
        try {
            DltEvent dltEvent = DltEvent.builder()
                .serviceName(serviceName)
                .errorMessage(ex.getMessage())
                .payload(new ObjectMapper().writeValueAsString(event))
                .createdAt(LocalDateTime.now())
                .build();

            log.info("Saving DLT event to database for jobId {} and recordId {}",
                event.getJobId(), event.getRecordId());
            dltEventRepository.save(dltEvent);

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
