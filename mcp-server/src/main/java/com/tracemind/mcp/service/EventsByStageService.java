package com.tracemind.mcp.service;

import com.tracemind.mcp.client.SplunkClient;
import com.tracemind.mcp.model.SplunkSearchRequest;
import com.tracemind.mcp.model.SplunkSearchResponse;
import com.tracemind.mcp.query.SplunkQueryBuilder;
import org.springframework.stereotype.Service;

@Service
public class EventsByStageService {

    private final SplunkQueryBuilder splunkQueryBuilder;
    private final SplunkClient splunkClient;

    public EventsByStageService(SplunkQueryBuilder splunkQueryBuilder, SplunkClient splunkClient) {
        this.splunkQueryBuilder = splunkQueryBuilder;
        this.splunkClient = splunkClient;
    }

    public SplunkSearchResponse getEventsByStage() {
        String query = splunkQueryBuilder.buildEventsByStageQuery();

        SplunkSearchRequest request = SplunkSearchRequest.builder()
                .query(query)
                .outputMode("json")
                .build();

        return splunkClient.execute(request);
    }
}
