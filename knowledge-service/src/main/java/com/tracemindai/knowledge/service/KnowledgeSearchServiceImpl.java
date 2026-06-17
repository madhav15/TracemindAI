package com.tracemindai.knowledge.service;

import com.tracemindai.common.contract.knowledge.KnowledgeSearchResult;
import com.tracemindai.knowledge.repository.KnowledgeDocumentRepository;
import com.tracemindai.knowledge.repository.KnowledgeSearchProjection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class KnowledgeSearchServiceImpl implements KnowledgeSearchService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeSearchServiceImpl.class);

    private final OpenAiEmbeddingService embeddingService;
    private final KnowledgeDocumentRepository repository;

    public KnowledgeSearchServiceImpl(OpenAiEmbeddingService embeddingService,
                                      KnowledgeDocumentRepository repository) {
        this.embeddingService = embeddingService;
        this.repository = repository;
    }

    @Override
    public List<KnowledgeSearchResult> search(String query, int limit) {
        List<Float> queryEmbedding = embeddingService.generateEmbedding(query);
        String vectorStr = embeddingService.embeddingToPgVector(queryEmbedding);

        List<KnowledgeSearchProjection> results = repository.findSimilarDocuments(vectorStr, limit);

        List<KnowledgeSearchResult> response = new ArrayList<>();
        for (KnowledgeSearchProjection row : results) {
            response.add(new KnowledgeSearchResult(
                    row.getDocumentName(),
                    row.getChunkId(),
                    row.getContent(),
                    row.getSimilarity()
            ));
        }

        log.info("Search for '{}' returned {} results", query, response.size());
        return response;
    }
}
