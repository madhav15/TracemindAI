package com.tracemindai.fileupload.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.fileupload")
public class FileUploadConfig {
    private String uploadDir;
    private long maxFileSize;
    private int maxConcurrentUploads;
}
