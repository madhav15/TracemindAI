package com.tracemindai.knowledge.ingestion;

import com.tracemindai.knowledge.entity.KnowledgeDocument;
import com.tracemindai.knowledge.repository.KnowledgeDocumentRepository;
import com.tracemindai.knowledge.service.OpenAiEmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class KnowledgeIngestionService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeIngestionService.class);

    private final MarkdownDocumentReader reader;
    private final DocumentChunker chunker;
    private final OpenAiEmbeddingService embeddingService;
    private final KnowledgeDocumentRepository repository;

    public KnowledgeIngestionService(MarkdownDocumentReader reader,
                                     DocumentChunker chunker,
                                     OpenAiEmbeddingService embeddingService,
                                     KnowledgeDocumentRepository repository) {
        this.reader = reader;
        this.chunker = chunker;
        this.embeddingService = embeddingService;
        this.repository = repository;
    }

    public void ingestKnowledgeBase() {
        log.info("Starting incremental knowledge ingestion from classpath:/knowledge/");

        List<Resource> documents = reader.listDocuments();
        Set<String> documentNamesOnDisk = documents.stream()
                .map(Resource::getFilename)
                .collect(Collectors.toSet());

        int newCount = 0;
        int modifiedCount = 0;
        int skippedCount = 0;
        int deletedCount = 0;

        // Process documents on disk
        for (Resource docResource : documents) {
            String documentName = docResource.getFilename();
            String content = reader.readDocument(docResource);
            String currentHash = DocumentHashUtil.calculateSha256(content);

            Optional<KnowledgeDocument> existing = repository.findFirstByDocumentName(documentName);

            if (existing.isEmpty()) {
                // Scenario 1: New document
                log.info("NEW DOCUMENT: {}", documentName);
                ingestDocument(documentName, content, currentHash);
                newCount++;
            } else {
                String storedHash = existing.get().getDocumentHash();
                if (storedHash != null && storedHash.equals(currentHash)) {
                    // Scenario 2: Unchanged document
                    log.info("UNCHANGED DOCUMENT: {} — Skipping ingestion", documentName);
                    skippedCount++;
                } else {
                    // Scenario 3: Modified document
                    log.info("MODIFIED DOCUMENT: {} — Re-ingesting", documentName);
                    repository.deleteByDocumentName(documentName);
                    ingestDocument(documentName, content, currentHash);
                    modifiedCount++;
                }
            }
        }

        // Scenario 4: Deleted documents
        List<KnowledgeDocument> allDocs = repository.findAll();
        Set<String> existingDocumentNames = allDocs.stream()
                .map(KnowledgeDocument::getDocumentName)
                .collect(Collectors.toSet());

        for (String existingName : existingDocumentNames) {
            if (!documentNamesOnDisk.contains(existingName)) {
                log.info("DELETED DOCUMENT: {} — Removing from vector store", existingName);
                repository.deleteByDocumentName(existingName);
                deletedCount++;
            }
        }

        log.info("Knowledge ingestion summary");
        log.info("  New Documents      : {}", newCount);
        log.info("  Modified Documents : {}", modifiedCount);
        log.info("  Skipped Documents  : {}", skippedCount);
        log.info("  Deleted Documents  : {}", deletedCount);
    }

    private void ingestDocument(String documentName, String content, String documentHash) {
        List<String> chunks = chunker.chunk(documentName, content);

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            List<Float> embedding = embeddingService.generateEmbedding(chunk);
            String vectorStr = embeddingService.embeddingToPgVector(embedding);

            KnowledgeDocument doc = KnowledgeDocument.builder()
                    .documentName(documentName)
                    .chunkId(i)
                    .content(chunk)
                    .documentHash(documentHash)
                    .createdAt(LocalDateTime.now())
                    .build();

            repository.save(doc);
            repository.updateEmbedding(doc.getId(), vectorStr);
        }

        log.info("  {} chunks ingested for {}", chunks.size(), documentName);
    }
}