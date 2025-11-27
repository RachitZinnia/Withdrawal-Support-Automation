package com.withdrawal.support.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyReportRow {
    private String camundaBusinessKey;
    private String camundaClientCode;
    private String camundaContractNumber;
    private String camundaStartTime;
    private String onBaseBusinessKey;
    private String onBaseClientCode;
    private String onBaseCaseQueue;
    private String onBaseContractNumber;
    private String match;  // "Matching" or "Not Matching"
    private String documentProcessingDate;
    private String agingInDays;
    private String onBasePendingCallOut;
    private String onBaseNotes;
    
    /**
     * Gets the business key to use (OnBase if available, otherwise Camunda)
     */
    public String getBusinessKeyToUse() {
        if (onBaseBusinessKey != null && !onBaseBusinessKey.trim().isEmpty()) {
            return onBaseBusinessKey;
        }
        return camundaBusinessKey;
    }
    
    /**
     * Checks if this row should be processed (Match = "Not Matching")
     */
    public boolean shouldProcess() {
        return "Not Matching".equalsIgnoreCase(match) || 
               "NotMatching".equalsIgnoreCase(match);
    }
}




