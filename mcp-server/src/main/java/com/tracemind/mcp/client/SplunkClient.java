package com.tracemind.mcp.client;

import com.tracemind.mcp.exception.SplunkClientException;
import com.tracemind.mcp.model.SplunkSearchRequest;
import com.tracemind.mcp.model.SplunkSearchResponse;

public interface SplunkClient {

    /**
     * Executes a Splunk search and returns the response.
     *
     * @param request the search request containing the SPL query and optional parameters
     * @return the search response containing the raw JSON and metadata
     * @throws SplunkClientException if the request fails
     */
    SplunkSearchResponse execute(SplunkSearchRequest request);
}
