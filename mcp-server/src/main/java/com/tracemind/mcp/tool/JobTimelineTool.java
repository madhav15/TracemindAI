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
            description = """
                    Purpose:
                    Retrieve a complete chronological timeline for a specific job using its business jobId.

                    Use this tool when the user wants to understand the lifecycle, execution history, processing stages,
                    failures, retries, or completion status of a specific job.

                    Example Questions:
                    - Show timeline for JOB-123
                    - What happened to JOB-123?
                    - Trace JOB-123 from start to finish
                    - Where did JOB-123 fail?

                    Returns:
                    All processing events for the given job sorted by timestamp, including service names, stages,
                    statuses, and log messages across the job's entire execution path.

                    Do Not Use:
                    - For record-level investigations — use get_record_timeline instead
                    - For system-wide failure analysis — use get_failed_jobs instead
                    - For a high-level overview of the job's business journey — use get_job_summary instead
                    - For email-specific failures — use get_failed_emails instead""")
    public SplunkSearchResponse getJobTimeline(
            @ToolParam(description = "The jobId to retrieve the timeline for", required = true) String jobId) {
        return jobTimelineService.getJobTimeline(jobId);
    }
}
