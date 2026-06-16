package com.tracemind.mcp.tool;

import com.tracemind.mcp.model.SplunkSearchResponse;
import com.tracemind.mcp.service.FailedJobsService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class FailedJobsTool {

    private final FailedJobsService failedJobsService;

    public FailedJobsTool(FailedJobsService failedJobsService) {
        this.failedJobsService = failedJobsService;
    }

    @Tool(name = "get_failed_jobs",
            description = """
                    Purpose:
                    Return all jobs across the system that have a FAILED status.

                    Use this tool when the user wants to see a system-wide view of failures — which jobs are failing,
                    how many failures are occurring, and the failure messages across all services and jobs.

                    Example Questions:
                    - Show me all failed jobs
                    - What jobs are currently failing?
                    - How many job failures have occurred today?
                    - List all recent failures across the system

                    Returns:
                    All events with status FAILED across every service and job, including jobId, service name,
                    stage, error message, and timestamp for each failure.

                    Do Not Use:
                    - For the timeline of a specific job — use get_job_timeline instead
                    - For email-specific failures only — use get_failed_emails instead
                    - For retry-related failures — use get_retry_events instead
                    - For dead-letter queue events — use get_dlt_events instead""")
    public SplunkSearchResponse getFailedJobs() {
        return failedJobsService.getFailedJobs();
    }
}
