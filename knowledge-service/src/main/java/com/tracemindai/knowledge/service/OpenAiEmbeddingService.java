package com.tracemindai.knowledge.service;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.embeddings.EmbeddingCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OpenAiEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiEmbeddingService.class);
    private static final String MODEL = "text-embedding-3-small";
    private static final long DIMENSIONS = 1536L;

    private final OpenAIClient client;

    public OpenAiEmbeddingService(@Value("${OPENAI_API_KEY:}") String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY is required for knowledge-service");
        }
        this.client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }

    public List<Float> generateEmbedding(String text) {
        var params = EmbeddingCreateParams.builder()
                .model(MODEL)
                .input(text)
                .dimensions(DIMENSIONS)
                .build();

        var response = client.embeddings().create(params);
        return response.data().get(0).embedding();
    }

    public String embeddingToPgVector(List<Float> embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding.get(i));
        }
        sb.append("]");
        return sb.toString();
    }
}