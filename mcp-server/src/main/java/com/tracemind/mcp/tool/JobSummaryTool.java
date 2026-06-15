package com.tracemind.mcp.tool;

import com.tracemind.mcp.model.SplunkSearchResponse;
import com.tracemind.mcp.service.JobSummaryService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class JobSummaryTool {

    private final JobSummaryService jobSummaryService;

    public JobSummaryTool(JobSummaryService jobSummaryService) {
        this.jobSummaryService = jobSummaryService;
    }

    @Tool(name = "get_job_summary",
            description = "Returns the overall business journey for a job.")
    public SplunkSearchResponse getJobSummary(
            @ToolParam(description = "The jobId to retrieve the summary for", required = true) String jobId) {
        return jobSummaryService.getJobSummary(jobId);
    }
}
