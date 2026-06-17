package com.tracemindai.knowledge.controller;

import com.tracemindai.common.contract.knowledge.KnowledgeSearchRequest;
import com.tracemindai.common.contract.knowledge.KnowledgeSearchResponse;
import com.tracemindai.knowledge.service.KnowledgeSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeController.class);

    private final KnowledgeSearchService knowledgeSearchService;

    public KnowledgeController(KnowledgeSearchService knowledgeSearchService) {
        this.knowledgeSearchService = knowledgeSearchService;
    }

    @PostMapping("/search")
    public KnowledgeSearchResponse search(@RequestBody KnowledgeSearchRequest request) {
        log.info("Search request: query='{}', limit={}", request.query(), request.limit());
        return new KnowledgeSearchResponse(
                knowledgeSearchService.search(request.query(), request.limit()));
    }
}
