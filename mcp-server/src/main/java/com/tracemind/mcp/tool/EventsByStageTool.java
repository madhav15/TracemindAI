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
            description = "Returns event statistics grouped by processing stage.")
    public SplunkSearchResponse getEventsByStage() {
        return eventsByStageService.getEventsByStage();
    }
}
