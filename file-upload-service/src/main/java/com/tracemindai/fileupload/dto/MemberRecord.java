package com.tracemindai.fileupload.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberRecord {
    private String memberId;
    private String name;
    private String mobile;
    private String email;
    private String communicationPreference;
}
