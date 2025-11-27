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
public class DailyReportProcessingResult {
    private int totalRowsInCsv;
    private int notMatchingRows;
    private int processedCases;
    private int successfulCases;
    private int failedCases;
    private int manualReviewRequired;
    
    @Builder.Default
    private List<String> businessKeysExtracted = new ArrayList<>();
    
    @Builder.Default
    private List<CaseProcessingDetail> details = new ArrayList<>();
    
    @Builder.Default
    private List<String> documentNumbersToCancel = new ArrayList<>();
    
    @Builder.Default
    private List<String> documentNumbersToReturning = new ArrayList<>();
    
    @Builder.Default
    private List<String> documentNumbersToComplete = new ArrayList<>();
    
    @Builder.Default
    private List<String> documentNumbersForManualReview = new ArrayList<>();
    
    private String message;
}




