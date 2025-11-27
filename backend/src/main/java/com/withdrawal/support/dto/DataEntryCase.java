package com.withdrawal.support.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataEntryCase {
    private String id;                    // Execution ID
    private String processInstanceId;     // Process Instance ID (used as case reference)
    private Boolean ended;
    private String tenantId;
}

