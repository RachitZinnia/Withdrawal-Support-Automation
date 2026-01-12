package com.withdrawal.support.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseStatusResponse {
    private int totalSubmitted;
    private int successCount;
    private int failedCount;
    private int createOscCount;
    private String message;
    
    @Builder.Default
    private List<String> successfulDocuments = new ArrayList<>();
    
    @Builder.Default
    private List<FailedDocument> failedDocuments = new ArrayList<>();
    
    @Builder.Default
    private List<CreateOscDocument> createOscDocuments = new ArrayList<>();
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedDocument {
        private String documentNumber;
        private String reason;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateOscDocument {
        private String documentNumber;
        private String reason;
        private String oscType;
    }
}


