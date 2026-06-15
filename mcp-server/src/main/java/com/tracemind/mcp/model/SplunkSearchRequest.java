package com.tracemind.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SplunkSearchRequest {

    private String query;
    private String earliestTime;
    private String latestTime;
    private Integer maxResults;
    private String outputMode;
}
