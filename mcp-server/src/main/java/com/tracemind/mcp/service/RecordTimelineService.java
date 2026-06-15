package com.tracemind.mcp.service;

import com.tracemind.mcp.client.SplunkClient;
import com.tracemind.mcp.model.SplunkSearchRequest;
import com.tracemind.mcp.model.SplunkSearchResponse;
import com.tracemind.mcp.query.SplunkQueryBuilder;
import org.springframework.stereotype.Service;

@Service
public class RecordTimelineService {

    private final SplunkQueryBuilder splunkQueryBuilder;
    private final SplunkClient splunkClient;

    public RecordTimelineService(SplunkQueryBuilder splunkQueryBuilder, SplunkClient splunkClient) {
        this.splunkQueryBuilder = splunkQueryBuilder;
        this.splunkClient = splunkClient;
    }

    public SplunkSearchResponse getRecordTimeline(String recordId) {
        String query = splunkQueryBuilder.buildRecordTimelineQuery(recordId);

        SplunkSearchRequest request = SplunkSearchRequest.builder()
                .query(query)
                .outputMode("json")
                .build();

        return splunkClient.execute(request);
    }
}
