package com.tracemind.mcp.config;

import com.tracemind.mcp.tool.JobTimelineTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider jobTimelineToolCallbackProvider(JobTimelineTool jobTimelineTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(jobTimelineTool)
                .build();
    }
}
