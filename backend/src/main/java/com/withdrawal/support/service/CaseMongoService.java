package com.withdrawal.support.service;

import com.withdrawal.support.model.CaseDocument;
import com.withdrawal.support.model.CaseInstanceDocument;
import com.withdrawal.support.model.CaseStatus;
import com.withdrawal.support.repository.CaseInstanceRepository;
import com.withdrawal.support.repository.CaseRepository;
import com.withdrawal.support.util.BusinessDaysCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaseMongoService {

    private final CaseRepository caseRepository;
    private final CaseInstanceRepository caseInstanceRepository;





    /**
     * Finds cases in progress that are older than specified days
     */
    public List<CaseDocument> findStaleInProgressCases(int daysThreshold) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysThreshold);
        log.info("Finding in-progress cases older than {} days (before {})", daysThreshold, cutoffDate);
        return caseRepository.findByStatusAndLastUpdatedBefore(CaseStatus.IN_PROGRESS, cutoffDate);
    }

    /**
     * Analyzes a case from MongoDB and returns comprehensive status information
     * This method queries MongoDB ONCE and extracts all needed information
     */
    public CaseAnalysisResult analyzeCaseFromMongo(String documentNumber, int businessDaysThreshold) {
        log.info("Analyzing MongoDB case_instance for document number: {}", documentNumber);
        
        Optional<CaseInstanceDocument> caseInstance = caseInstanceRepository.findByDocumentNumber(documentNumber);
        
        if (caseInstance.isEmpty()) {
            log.info("Case not found in MongoDB for document number: {} - treating as not in progress", documentNumber);
            return CaseAnalysisResult.builder()
                    .found(false)
                    .caseStatus(null)
                    .isInProgress(false)
                    .isStale(false)
                    .lastUpdated(null)
                    .build();
        }
        
        CaseInstanceDocument document = caseInstance.get();
        String caseStatus = document.getCaseStatus();
        
        // Check caseStatus field first (preferred), fallback to status field
        String statusToCheck = caseStatus != null ? caseStatus : document.getStatus();
        
        // Check if IN_PROGRESS
        boolean isInProgress = "IN_PROGRESS".equalsIgnoreCase(statusToCheck) || 
                              "in progress".equalsIgnoreCase(statusToCheck) ||
                              "in_progress".equalsIgnoreCase(statusToCheck) ||
                              "inprogress".equalsIgnoreCase(statusToCheck);
        
        // Check if stale (only if IN_PROGRESS) - using BUSINESS DAYS
        boolean isStale = false;
        LocalDateTime lastUpdated = document.getUpdatedAt();
        
        if (isInProgress) {
            if (lastUpdated == null) {
                lastUpdated = document.getCreatedAt();
            }
            
            if (lastUpdated != null) {
                // Use business days calculation (excluding weekends)
                isStale = BusinessDaysCalculator.isOlderThanBusinessDays(lastUpdated, businessDaysThreshold);
            }
        }
        
        log.info("Case analysis - caseStatus: {}, status: {}, isInProgress: {}, isStale: {}, lastUpdated: {}", 
                caseStatus, document.getStatus(), isInProgress, isStale, lastUpdated);
        
        return CaseAnalysisResult.builder()
                .found(true)
                .caseStatus(caseStatus)
                .statusField(document.getStatus())
                .isInProgress(isInProgress)
                .isStale(isStale)
                .lastUpdated(lastUpdated)
                .createdDate(document.getCreatedAt())
                .build();
    }
    
    /**
     * Result object containing all analysis information from a single MongoDB query
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CaseAnalysisResult {
        private boolean found;              // Was the case found in MongoDB?
        private String caseStatus;          // The caseStatus field value
        private String statusField;         // The status field value (fallback)
        private boolean isInProgress;       // Is the case IN_PROGRESS?
        private boolean isStale;            // Is it older than threshold?
        private LocalDateTime lastUpdated;  // Last updated timestamp
        private LocalDateTime createdDate;  // Created timestamp
    }
    


}

