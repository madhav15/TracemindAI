package com.tracemind.mcp.config;

import com.tracemind.mcp.tool.*;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider toolCallbackProvider(
            JobTimelineTool jobTimelineTool,
            RecordTimelineTool recordTimelineTool,
            FailedJobsTool failedJobsTool,
            FailedEmailsTool failedEmailsTool,
            RetryEventsTool retryEventsTool,
            DltEventsTool dltEventsTool,
            EventsByServiceTool eventsByServiceTool,
            EventsByStageTool eventsByStageTool,
            ProcessingDurationTool processingDurationTool,
            JobSummaryTool jobSummaryTool,
            SearchKnowledgeTool searchKnowledgeTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(
                        jobTimelineTool,
                        recordTimelineTool,
                        failedJobsTool,
                        failedEmailsTool,
                        retryEventsTool,
                        dltEventsTool,
                        eventsByServiceTool,
                        eventsByStageTool,
                        processingDurationTool,
                        jobSummaryTool,
                        searchKnowledgeTool)
                .build();
    }
}
