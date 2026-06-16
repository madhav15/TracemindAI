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
            description = """
                    Purpose:
                    Return email processing events that failed, scoped specifically to the email-service.

                    Use this tool when the user asks about email failures, undelivered emails, email validation errors,
                    or email sending problems — this tool isolates failures to the email service only.

                    Example Questions:
                    - Show me failed emails
                    - Why are emails not being delivered?
                    - What email failures occurred today?
                    - List all email sending errors

                    Returns:
                    Failed email processing events from the email-service including jobId, failure reason, timestamp,
                    and error details for each email failure.

                    Do Not Use:
                    - For broad system failures across all services — use get_failed_jobs instead
                    - For the timeline of a specific job — use get_job_timeline instead
                    - For retry events on failed emails — use get_retry_events instead""")
    public SplunkSearchResponse getFailedEmails() {
        return failedEmailsService.getFailedEmails();
    }
}
