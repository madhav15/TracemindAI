package com.tracemind.mcp.client;

import com.tracemindai.common.contract.knowledge.KnowledgeSearchRequest;
import com.tracemindai.common.contract.knowledge.KnowledgeSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class KnowledgeClient {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeClient.class);

    private final RestClient restClient;

    public KnowledgeClient(@Value("${knowledge.service.url}") String knowledgeServiceUrl,
                           RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl(knowledgeServiceUrl)
                .build();
    }

    public KnowledgeSearchResponse search(String query, int limit) {
        log.info("Calling knowledge service: query='{}', limit={}", query, limit);
        return restClient.post()
                .uri("/api/knowledge/search")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new KnowledgeSearchRequest(query, limit))
                .retrieve()
                .body(KnowledgeSearchResponse.class);
    }
}
