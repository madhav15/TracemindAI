package com.tracemind.mcp.tool;

import com.tracemind.mcp.model.SplunkSearchResponse;
import com.tracemind.mcp.service.RecordTimelineService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class RecordTimelineTool {

    private final RecordTimelineService recordTimelineService;

    public RecordTimelineTool(RecordTimelineService recordTimelineService) {
        this.recordTimelineService = recordTimelineService;
    }

    @Tool(name = "get_record_timeline",
            description = "Returns the complete lifecycle of a record from Splunk logs.")
    public SplunkSearchResponse getRecordTimeline(
            @ToolParam(description = "The recordId to retrieve the timeline for", required = true) String recordId) {
        return recordTimelineService.getRecordTimeline(recordId);
    }
}
