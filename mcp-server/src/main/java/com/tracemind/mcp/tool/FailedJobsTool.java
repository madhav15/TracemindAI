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
            description = "Returns failed jobs from Splunk logs.")
    public SplunkSearchResponse getFailedJobs() {
        return failedJobsService.getFailedJobs();
    }
}
