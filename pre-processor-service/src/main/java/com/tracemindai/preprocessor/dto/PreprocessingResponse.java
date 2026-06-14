package com.tracemindai.preprocessor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreprocessingResponse {
    private String processId;
    private String fileId;
    private String status;
    private String processedAt;
}
