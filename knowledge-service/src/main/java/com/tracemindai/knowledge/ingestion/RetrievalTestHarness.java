package com.tracemindai.knowledge.ingestion;

import com.tracemindai.knowledge.repository.KnowledgeDocumentRepository;
import com.tracemindai.knowledge.service.KnowledgeSearchService;
import com.tracemindai.knowledge.service.OpenAiEmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import java.util.List;
import java.util.Map;

@SpringBootApplication
@ComponentScan(basePackages = "com.tracemindai.knowledge")
public class RetrievalTestHarness {

    private static final Logger log = LoggerFactory.getLogger(RetrievalTestHarness.class);
    private static final int TOP_K = 3;

    public static void main(String[] args) {
        System.setProperty("spring.main.web-application-type", "none");
        System.setProperty("spring.main.banner-mode", "off");

        try (ConfigurableApplicationContext ctx = SpringApplication.run(RetrievalTestHarness.class, args)) {
            KnowledgeSearchService searchService = ctx.getBean(KnowledgeSearchService.class);
            KnowledgeDocumentRepository repository = ctx.getBean(KnowledgeDocumentRepository.class);

            long count = repository.count();
            if (count == 0) {
                log.error("No documents in knowledge_document table. Run ingestion first.");
                System.exit(1);
            }
            log.info("Running retrieval tests against {} indexed chunks", count);

            String[] queries = {
                    "How does retry work?",
                    "What is DLT?",
                    "How does email-service work?",
                    "Why are logs not embedded?",
                    "How does a record flow through the system?"
            };

            for (String q : queries) {
                log.info("=== Query: {} ===", q);
                try {
                    List<Map<String, Object>> results = searchService.search(q, TOP_K);
                    if (results.isEmpty()) {
                        log.info("  (no results)");
                        continue;
                    }
                    for (int i = 0; i < results.size(); i++) {
                        Map<String, Object> r = results.get(i);
                        log.info("  #{}. doc={} chunk={} similarity={}",
                                i + 1, r.get("documentName"), r.get("chunkId"), r.get("similarity"));
                        String content = (String) r.get("content");
                        String snippet = content.length() > 120 ? content.substring(0, 120) + "..." : content;
                        log.info("      snippet: {}", snippet.replace("\n", " "));
                    }
                } catch (Exception e) {
                    log.error("Query '{}' failed: {}", q, e.getMessage());
                }
            }
        }
    }
}
