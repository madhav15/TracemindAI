package com.tracemindai.knowledge.ingestion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@RequiredArgsConstructor
@Slf4j
public class KnowledgeStartupLoader {

    private final KnowledgeIngestionService ingestionService;

    @PostConstruct
    public void initialize() {
        log.info("Starting incremental knowledge ingestion");
        ingestionService.ingestKnowledgeBase();
        log.info("Knowledge ingestion complete");
    }
}