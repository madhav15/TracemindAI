package com.tracemind.mcp.tool;

import com.tracemind.mcp.model.SplunkSearchResponse;
import com.tracemind.mcp.service.EventsByServiceService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class EventsByServiceTool {

    private final EventsByServiceService eventsByServiceService;

    public EventsByServiceTool(EventsByServiceService eventsByServiceService) {
        this.eventsByServiceService = eventsByServiceService;
    }

    @Tool(name = "get_events_by_service",
            description = """
                    Purpose:
                    Return aggregate event counts grouped by service across the entire system.

                    Use this tool when the user wants a high-level view of system activity — which services are
                    processing the most events, how event volume is distributed across services, or to identify
                    services with unusually high or low activity.

                    Example Questions:
                    - Show event counts by service
                    - Which service is processing the most events?
                    - What is the event distribution across services?
                    - How many events did each service handle?

                    Returns:
                    A count of events per service, providing a bird's-eye view of event volume distribution
                    across all services in the system.

                    Do Not Use:
                    - For event counts grouped by processing stage — use get_events_by_stage instead
                    - For details about specific events within a service — use get_job_timeline or get_record_timeline
                    - For failure counts by service — combine with get_failed_jobs instead""")
    public SplunkSearchResponse getEventsByService() {
        return eventsByServiceService.getEventsByService();
    }
}
