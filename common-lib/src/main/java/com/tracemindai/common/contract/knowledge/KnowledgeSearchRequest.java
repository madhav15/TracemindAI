package com.tracemindai.common.contract.knowledge;

public record KnowledgeSearchRequest(
        String query,
        int limit
) {
    public KnowledgeSearchRequest {
        if (limit <= 0) limit = 5;
    }
}
