package com.tracemindai.knowledge.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DocumentChunker {

    private static final Logger log = LoggerFactory.getLogger(DocumentChunker.class);
    private static final int TARGET_MAX_WORDS = 250;

    public List<String> chunk(String documentName, String content) {
        List<String> sections = splitByHeadings(content);

        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        int currentWords = 0;

        for (String section : sections) {
            int sectionWords = countWords(section);

            if (sectionWords > TARGET_MAX_WORDS) {
                if (currentWords > 0) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                    currentWords = 0;
                }
                chunks.addAll(splitOversizedSection(section));
                continue;
            }

            if (currentWords > 0 && currentWords + sectionWords > TARGET_MAX_WORDS) {
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
                currentWords = 0;
            }

            if (currentWords > 0) {
                currentChunk.append("\n\n");
            }
            currentChunk.append(section);
            currentWords += sectionWords;
        }

        if (currentWords > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        log.info("Chunked '{}' ({} words) into {} chunks", documentName, countWords(content), chunks.size());
        return chunks;
    }

    private List<String> splitByHeadings(String content) {
        List<String> sections = new ArrayList<>();
        String[] lines = content.split("\n");
        StringBuilder current = new StringBuilder();

        for (String line : lines) {
            if (line.trim().startsWith("#") && !current.isEmpty()) {
                sections.add(current.toString().trim());
                current = new StringBuilder();
            }
            if (!current.isEmpty()) {
                current.append("\n");
            }
            current.append(line);
        }

        if (!current.isEmpty()) {
            sections.add(current.toString().trim());
        }

        return sections;
    }

    private List<String> splitOversizedSection(String section) {
        String[] paragraphs = section.split("\n\n");
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int currentWords = 0;

        for (String para : paragraphs) {
            int paraWords = countWords(para);
            if (currentWords > 0 && currentWords + paraWords > TARGET_MAX_WORDS) {
                result.add(current.toString().trim());
                current = new StringBuilder();
                currentWords = 0;
            }
            if (currentWords > 0) current.append("\n\n");
            current.append(para);
            currentWords += paraWords;
        }

        if (currentWords > 0) {
            result.add(current.toString().trim());
        }
        return result;
    }

    private int countWords(String text) {
        return text.split("\\s+").length;
    }
}
