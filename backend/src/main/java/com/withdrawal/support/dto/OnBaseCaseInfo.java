package com.withdrawal.support.dto;

import com.withdrawal.support.model.OnBaseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnBaseCaseInfo {
    private String caseId;
    private OnBaseStatus status;
    private LocalDateTime lastUpdated;
    private List<String> documents;
    private String notes;
    private boolean hasAllRequiredDocuments;
}





