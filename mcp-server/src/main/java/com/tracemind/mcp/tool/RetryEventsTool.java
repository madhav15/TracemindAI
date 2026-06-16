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
            description = """
                    Purpose:
                    Return all events that have been retried at least once during processing.

                    Use this tool when the user wants to investigate resilience and recovery patterns — which jobs or
                    records are being retried, how many retry attempts are occurring, and whether retries are succeeding
                    or leading to eventual failures.

                    Example Questions:
                    - Show me all retry events
                    - What operations are being retried?
                    - How many retries are happening in the system?
                    - Are retries succeeding or failing?

                    Returns:
                    All events where retryCount > 0, including jobId, service, retry count, stage, status, and
                    timestamp for each retried event.

                    Do Not Use:
                    - For first-time failures with no retries — use get_failed_jobs instead
                    - For events that ended up in the dead letter queue — use get_dlt_events instead (those may overlap
                      if a retried event was ultimately sent to DLT)
                    - For the timeline of a specific job — use get_job_timeline instead""")
    public SplunkSearchResponse getRetryEvents() {
        return retryEventsService.getRetryEvents();
    }
}
