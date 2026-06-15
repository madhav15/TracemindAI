package com.tracemind.mcp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "splunk")
public class SplunkProperties {

    private String baseUrl;
    private String username;
    private String password;
    private String index;
    private boolean verifySsl = true;
}
