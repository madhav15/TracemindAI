package com.tracemindai.knowledge.service;

import com.tracemindai.knowledge.repository.KnowledgeDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class KnowledgeSearchService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeSearchService.class);

    private final OpenAiEmbeddingService embeddingService;
    private final KnowledgeDocumentRepository repository;

    public KnowledgeSearchService(OpenAiEmbeddingService embeddingService,
                                  KnowledgeDocumentRepository repository) {
        this.embeddingService = embeddingService;
        this.repository = repository;
    }

    public List<Map<String, Object>> search(String query, int topK) {
        List<Float> queryEmbedding = embeddingService.generateEmbedding(query);
        String vectorStr = embeddingService.embeddingToPgVector(queryEmbedding);

        List<Object[]> results = repository.findSimilarDocuments(vectorStr, topK);

        List<Map<String, Object>> response = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("documentName", row[1]);
            item.put("chunkId", row[2]);
            item.put("content", row[3]);
            item.put("similarity", row[4]);
            response.add(item);
        }

        log.info("Search for '{}' returned {} results", query, response.size());
        return response;
    }
}