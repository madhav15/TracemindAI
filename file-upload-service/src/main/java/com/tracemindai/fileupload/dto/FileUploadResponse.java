package com.tracemindai.fileupload.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileUploadResponse {
    @JsonProperty("jobId")
    private String jobId;

    @JsonProperty("fileName")
    private String fileName;

    @JsonProperty("totalRecords")
    private Integer totalRecords;

    @JsonProperty("status")
    private String status;
}
