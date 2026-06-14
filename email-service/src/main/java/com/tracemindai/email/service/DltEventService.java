package com.tracemindai.email.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracemindai.common.event.EmailRequestEvent;
import com.tracemindai.email.entity.DltEvent;
import com.tracemindai.email.repository.DltEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class DltEventService {

    private final DltEventRepository dltEventRepository;

    public void save(String serviceName, EmailRequestEvent event, Exception ex) {
        try {
            DltEvent dltEvent = DltEvent.builder()
                    .errorMessage(ex.getMessage())
                    .payload(new ObjectMapper().writeValueAsString(event))
                    .serviceName(serviceName)
                    .createdAt(LocalDateTime.now())
                    .build();

            log.info("Saving DLT event to database Job {} and record {}", event.getJobId(), event.getRecordId());
            dltEventRepository.save(dltEvent);

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }
}
