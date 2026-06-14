package com.tracemindai.common.event;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecordCreatedEvent {

    private String jobId;

    private String recordId;

    private String memberId;

    private String communicationPreference;

    private String correlationId;

    private String traceId;

    private String email;

    private String mobile;
}
