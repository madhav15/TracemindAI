package com.tracemindai.preprocessor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.preprocessor")
public class PreProcessorConfig {
    private int processingThreads;
    private long processingTimeout;
}
