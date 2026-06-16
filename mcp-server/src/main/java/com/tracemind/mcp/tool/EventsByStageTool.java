package com.tracemind.mcp.tool;

import com.tracemind.mcp.model.SplunkSearchResponse;
import com.tracemind.mcp.service.EventsByStageService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class EventsByStageTool {

    private final EventsByStageService eventsByStageService;

    public EventsByStageTool(EventsByStageService eventsByStageService) {
        this.eventsByStageService = eventsByStageService;
    }

    @Tool(name = "get_events_by_stage",
            description = """
                    Purpose:
                    Return aggregate event counts grouped by processing stage across the entire system.

                    Use this tool when the user wants to understand the processing pipeline's health — which stages
                    (e.g., validation, transformation, enrichment, delivery) have the most or fewest events, where
                    bottlenecks or backlogs might exist, or how events progress through the pipeline stages.

                    Example Questions:
                    - Show event counts by stage
                    - Which processing stage has the most events?
                    - Where are events getting stuck in the pipeline?
                    - What is the event distribution across stages?

                    Returns:
                    A count of events per processing stage, providing a view of how events are distributed
                    across the pipeline's stages.

                    Do Not Use:
                    - For event counts grouped by service — use get_events_by_service instead
                    - For details about a specific event's stage progression — use get_job_timeline or get_record_timeline
                    - For failure counts by stage — combine with get_failed_jobs instead""")
    public SplunkSearchResponse getEventsByStage() {
        return eventsByStageService.getEventsByStage();
    }
}
