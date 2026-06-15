package com.tracemind.mcp.tool;

import com.tracemind.mcp.model.SplunkSearchResponse;
import com.tracemind.mcp.service.RetryEventsService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class RetryEventsTool {

    private final RetryEventsService retryEventsService;

    public RetryEventsTool(RetryEventsService retryEventsService) {
        this.retryEventsService = retryEventsService;
    }

    @Tool(name = "get_retry_events",
            description = "Returns retry attempts recorded in Splunk.")
    public SplunkSearchResponse getRetryEvents() {
        return retryEventsService.getRetryEvents();
    }
}
