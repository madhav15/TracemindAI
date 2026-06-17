package com.tracemindai.common.contract.knowledge;

import java.util.List;

public record KnowledgeSearchResponse(
        List<KnowledgeSearchResult> results
) {}
