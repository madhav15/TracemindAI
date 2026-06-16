package com.tracemindai.knowledge.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Component
public class MarkdownDocumentReader {

    private static final Logger log = LoggerFactory.getLogger(MarkdownDocumentReader.class);
    private static final String KNOWLEDGE_PATH = "classpath:/knowledge/*.md";

    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    public String readDocument(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read document: " + resource.getFilename(), e);
        }
    }

    public List<Resource> listDocuments() {
        try {
            Resource[] resources = resolver.getResources(KNOWLEDGE_PATH);
            return Arrays.stream(resources)
                    .sorted(Comparator.comparing(r -> r.getFilename()))
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Failed to list documents from: " + KNOWLEDGE_PATH, e);
        }
    }
}
