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
            description = """
                    Purpose:
                    Return a high-level overview of a job's business journey — its overall status, outcome, and
                    key milestones — rather than every individual processing event.

                    Use this tool when the user wants a summary or status of a job without the full chronological detail.
                    This is the right tool for a quick status check or business-level question about what a job
                    accomplished, rather than a deep technical trace.

                    Example Questions:
                    - What is the status of JOB-123?
                    - Give me a summary of JOB-123
                    - Did JOB-123 complete successfully?
                    - What was the outcome of JOB-123?

                    Returns:
                    A condensed view of the job's processing — key milestones, final status, and business outcome —
                    suitable for answering status-check and summary questions.

                    Do Not Use:
                    - For a detailed chronological trace of every event — use get_job_timeline instead
                    - For record-level investigations — use get_record_timeline instead
                    - For system-wide job statistics — use get_failed_jobs or get_events_by_service instead""")
    public SplunkSearchResponse getJobSummary(
            @ToolParam(description = "The jobId to retrieve the summary for", required = true) String jobId) {
        return jobSummaryService.getJobSummary(jobId);
    }
}
