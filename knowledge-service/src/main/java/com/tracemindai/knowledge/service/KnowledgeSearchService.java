package com.tracemindai.knowledge.service;

import com.tracemindai.common.contract.knowledge.KnowledgeSearchResult;

import java.util.List;

public interface KnowledgeSearchService {

    List<KnowledgeSearchResult> search(String query, int limit);
}
