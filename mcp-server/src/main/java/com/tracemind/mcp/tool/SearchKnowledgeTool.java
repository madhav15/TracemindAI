package com.tracemind.mcp.tool;

import com.tracemind.mcp.client.KnowledgeClient;
import com.tracemindai.common.contract.knowledge.KnowledgeSearchResponse;
import com.tracemindai.common.contract.knowledge.KnowledgeSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SearchKnowledgeTool {

    private static final Logger log = LoggerFactory.getLogger(SearchKnowledgeTool.class);
    private static final int DEFAULT_LIMIT = 5;

    private final KnowledgeClient knowledgeClient;

    public SearchKnowledgeTool(KnowledgeClient knowledgeClient) {
        this.knowledgeClient = knowledgeClient;
    }

    @Tool(name = "search_knowledge",
            description = """
                    Search organizational knowledge and support documentation.

                    Use this tool when the user asks questions about system architecture, policies,
                    processing logic, error handling, or any operational knowledge that may be
                    documented in the knowledge base.

                    Example Questions:
                    - How does retry work?
                    - What is the DLT policy?
                    - How does the email service handle failures?
                    - What is the record processing flow?

                    Returns:
                    Relevant knowledge chunks grouped by document, ranked by semantic similarity
                    to the query.""")
    public String searchKnowledge(
            @ToolParam(description = "The search query describing what knowledge to retrieve") String query) {
        log.info("search_knowledge called with query: '{}'", query);

        KnowledgeSearchResponse response = knowledgeClient.search(query, DEFAULT_LIMIT);

        if (response == null || response.results().isEmpty()) {
            return "No relevant knowledge found.";
        }

        Map<String, List<KnowledgeSearchResult>> grouped = response.results().stream()
                .collect(Collectors.groupingBy(
                        KnowledgeSearchResult::documentName,
                        LinkedHashMap::new,
                        Collectors.toList()));

        StringBuilder sb = new StringBuilder();
        for (var entry : grouped.entrySet()) {
            sb.append("Document: ").append(entry.getKey()).append("\n\n");
            for (KnowledgeSearchResult chunk : entry.getValue()) {
                sb.append(chunk.content().trim()).append("\n\n");
            }
            sb.append("---\n\n");
        }

        return sb.toString().trim();
    }
}
