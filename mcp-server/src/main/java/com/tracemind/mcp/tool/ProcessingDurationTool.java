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
            description = "Returns processing duration information between initial consumption and completion.")
    public SplunkSearchResponse getProcessingDuration() {
        return processingDurationService.getProcessingDuration();
    }
}
