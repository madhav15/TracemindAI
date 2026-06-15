package com.tracemind.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SplunkSearchResponse {

    private boolean success;
    private String rawResponse;
    private Integer resultCount;
    private String searchId;
}
