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
            description = """
                    Purpose:
                    Retrieve the complete lifecycle of a specific data record using its recordId.

                    Use this tool when the user wants to trace how an individual record (e.g., a database row, a message,
                    or an entity) flowed through the system — including which services processed it, what stages it passed
                    through, and any errors or transformations applied to it.

                    Example Questions:
                    - Show timeline for record REC-456
                    - Trace the lifecycle of record REC-456
                    - What happened to record REC-456?
                    - Which services touched record REC-456?

                    Returns:
                    All processing events for the given record sorted by timestamp, including the services that handled
                    it, processing stages, statuses, and associated log messages.

                    Do Not Use:
                    - For job-level investigations — use get_job_timeline instead (a job may contain many records)
                    - For system-wide failure analysis — use get_failed_jobs instead
                    - For aggregate statistics across records — use get_events_by_service or get_events_by_stage instead""")
    public SplunkSearchResponse getRecordTimeline(
            @ToolParam(description = "The recordId to retrieve the timeline for", required = true) String recordId) {
        return recordTimelineService.getRecordTimeline(recordId);
    }
}
