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
            description = "Returns events that reached the Dead Letter Queue.")
    public SplunkSearchResponse getDltEvents() {
        return dltEventsService.getDltEvents();
    }
}
