package com.tracemindai.knowledge.repository;

public interface KnowledgeSearchProjection {
    String getDocumentName();
    Integer getChunkId();
    String getContent();
    Double getSimilarity();
}