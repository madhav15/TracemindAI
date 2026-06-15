package com.tracemind.mcp.tool;

import com.tracemind.mcp.model.SplunkSearchResponse;
import com.tracemind.mcp.service.FailedEmailsService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class FailedEmailsTool {

    private final FailedEmailsService failedEmailsService;

    public FailedEmailsTool(FailedEmailsService failedEmailsService) {
        this.failedEmailsService = failedEmailsService;
    }

    @Tool(name = "get_failed_emails",
            description = "Returns failed email processing events including validation and technical failures.")
    public SplunkSearchResponse getFailedEmails() {
        return failedEmailsService.getFailedEmails();
    }
}
