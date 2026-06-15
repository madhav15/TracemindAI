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
            description = "Returns event statistics grouped by service.")
    public SplunkSearchResponse getEventsByService() {
        return eventsByServiceService.getEventsByService();
    }
}
