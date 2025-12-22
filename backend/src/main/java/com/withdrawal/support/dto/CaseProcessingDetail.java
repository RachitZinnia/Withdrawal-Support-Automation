package com.withdrawal.support.dto;

import com.withdrawal.support.model.CaseCategory;
import com.withdrawal.support.model.CaseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseProcessingDetail {
    private String caseReference;
    private String caseId;
    private String clientCode;
    private String documentNumber;
    private String onbaseStatus;
    private CaseCategory category;
    private CaseStatus status;
    private String action;
    private String message;
    private LocalDateTime processedAt;
    private boolean requiresManualReview;
    private String reviewReason;
}

