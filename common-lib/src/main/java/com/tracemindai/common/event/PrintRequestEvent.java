package com.tracemindai.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrintRequestEvent {

    private String jobId;
    private String recordId;
    private String memberId;
    private String correlationId;
    private String traceId;
    private String email;
    private String mobile;

}
