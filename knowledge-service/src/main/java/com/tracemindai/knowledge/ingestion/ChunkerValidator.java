package com.tracemindai.knowledge.ingestion;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ChunkerValidator {

    public static void main(String[] args) throws Exception {
        DocumentChunker chunker = new DocumentChunker();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:/knowledge/*.md");

        List<Resource> sorted = Arrays.stream(resources)
                .sorted(Comparator.comparing(Resource::getFilename))
                .toList();

        Map<String, Integer> counts = new LinkedHashMap<>();
        int total = 0;

        for (Resource r : sorted) {
            String name = r.getFilename();
            String content = r.getContentAsString(StandardCharsets.UTF_8);
            List<String> chunks = chunker.chunk(name, content);
            counts.put(name, chunks.size());
            total += chunks.size();
        }

        System.out.println("=== Per-document chunk counts ===");
        counts.forEach((k, v) -> System.out.println(k + " -> " + v));
        System.out.println("=== TOTAL CHUNKS: " + total + " ===");

        if (total < 50 || total > 120) {
            System.err.println("WARN: total " + total + " is outside target range [50, 120]");
        } else {
            System.out.println("OK: total within target range [50, 120]");
        }
    }
}