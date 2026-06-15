package com.tracemind.mcp.tool;

import com.tracemind.mcp.model.SplunkSearchResponse;
import com.tracemind.mcp.service.JobTimelineService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class JobTimelineTool {

    private final JobTimelineService jobTimelineService;

    public JobTimelineTool(JobTimelineService jobTimelineService) {
        this.jobTimelineService = jobTimelineService;
    }

    @Tool(name = "get_job_timeline",
            description = "Returns the complete timeline for the supplied jobId by querying Splunk.")
    public SplunkSearchResponse getJobTimeline(
            @ToolParam(description = "The jobId to retrieve the timeline for", required = true) String jobId) {
        return jobTimelineService.getJobTimeline(jobId);
    }
}
