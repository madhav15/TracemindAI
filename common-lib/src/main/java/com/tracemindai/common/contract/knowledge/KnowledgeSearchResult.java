package com.tracemindai.common.contract.knowledge;

public record KnowledgeSearchResult(
        String documentName,
        Integer chunkId,
        String content,
        Double similarityScore
) {}
