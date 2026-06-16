package com.tracemind.mcp.tool;

import com.tracemind.mcp.model.SplunkSearchResponse;
import com.tracemind.mcp.service.DltEventsService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class DltEventsTool {

    private final DltEventsService dltEventsService;

    public DltEventsTool(DltEventsService dltEventsService) {
        this.dltEventsService = dltEventsService;
    }

    @Tool(name = "get_dlt_events",
            description = """
                    Purpose:
                    Return all events that were sent to the Dead Letter Topic (DLT) — the final destination for
                    messages that could not be processed after all retries and recovery attempts were exhausted.

                    Use this tool when the user wants to investigate unrecoverable failures, poisoned messages, or events
                    that have been abandoned by normal processing and require manual intervention.

                    Example Questions:
                    - Show me dead letter events
                    - What messages are in the DLT?
                    - Which jobs ended up in the dead letter queue?
                    - What events require manual intervention?

                    Returns:
                    All events with action DLT, including jobId, service, failure reason, original error, and
                    timestamp — representing events that exhausted all retries and were routed to the dead letter.

                    Do Not Use:
                    - For retryable failures still in progress — use get_retry_events instead
                    - For general failures that may recover — use get_failed_jobs instead
                    - For email-specific dead letters — use get_failed_emails instead (then correlate with DLT)""")
    public SplunkSearchResponse getDltEvents() {
        return dltEventsService.getDltEvents();
    }
}
