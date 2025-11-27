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
public class ProcessingResult {
    private int totalCases;
    private int successfulCases;
    private int failedCases;
    private int manualReviewRequired;
    
    @Builder.Default
    private List<CaseProcessingDetail> details = new ArrayList<>();
    
    // Document numbers to cancel in Camunda
    @Builder.Default
    private List<String> documentNumbersToCancel = new ArrayList<>();

    //Document numbers for CP Returning
    @Builder.Default
    private List<String> documentNumbersToReturning = new ArrayList<>();

    //Document numbers for DV POST COMPLETE
    @Builder.Default
    private List<String> documentNumbersToComplete = new ArrayList<>();
    
    // Document numbers requiring manual review (IN_PROGRESS but stale)
    @Builder.Default
    private List<String> documentNumbersForManualReview = new ArrayList<>();
    
    private String message;
}

