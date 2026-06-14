package com.tracemindai.preprocessor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreprocessingRequest {
    @NotBlank(message = "File ID cannot be blank")
    private String fileId;

    private String processingType;
}
