package com.withdrawal.support.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseDetails {
    private String caseId;               // OnBase Case ID (from Camunda variable)
    private String caseReference;        // Process Instance ID
    private String clientCode;           // Client Code (from Camunda variable)
    private Map<String, Object> clientVariables;
    private String accountNumber;
    private String clientName;
    private String withdrawalType;
}

