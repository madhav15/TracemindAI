package com.tracemind.mcp.tool;

import com.tracemind.mcp.model.SplunkSearchResponse;
import com.tracemind.mcp.service.ProcessingDurationService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class ProcessingDurationTool {

    private final ProcessingDurationService processingDurationService;

    public ProcessingDurationTool(ProcessingDurationService processingDurationService) {
        this.processingDurationService = processingDurationService;
    }

    @Tool(name = "get_processing_duration",
            description = """
                    Purpose:
                    Return all events with their timestamps to enable analysis of how long processing takes —
                    from initial consumption through each stage to final completion.

                    Use this tool when the user asks about processing speed, latency, bottlenecks, or end-to-end
                    duration — for example, how long jobs take to complete, which stages are slowest, or whether
                    processing times are degrading.

                    Example Questions:
                    - How long does processing take?
                    - What is the average processing duration?
                    - Which stage takes the longest?
                    - Is processing getting slower over time?

                    Returns:
                    All events across the system with timestamps, enabling the caller to compute duration metrics
                    (time from first event to last event per jobId, per-stage latency, etc.).

                    Do Not Use:
                    - For a specific job's duration — use get_job_timeline instead and calculate from the response
                    - For aggregate counts by service or stage — use get_events_by_service or get_events_by_stage
                    - For failure-only timing — combine with get_failed_jobs instead""")
    public SplunkSearchResponse getProcessingDuration() {
        return processingDurationService.getProcessingDuration();
    }
}
