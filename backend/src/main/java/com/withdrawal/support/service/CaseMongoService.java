package com.withdrawal.support.service;

import com.withdrawal.support.model.CaseDocument;
import com.withdrawal.support.model.CaseInstanceDocument;
import com.withdrawal.support.model.CaseStatus;
import com.withdrawal.support.repository.CaseInstanceRepository;
import com.withdrawal.support.repository.CaseRepository;
import com.withdrawal.support.util.BusinessDaysCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaseMongoService {

    private final CaseRepository caseRepository;
    private final CaseInstanceRepository caseInstanceRepository;


    @Autowired
    UsBusinessDayService usBusinessDayService;


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
    public CaseAnalysisResult analyzeCaseFromMongo(String documentNumber, int businessDaysThreshold, String onbaseTaskId) {
        log.info("Analyzing MongoDB case_instance for document number: {}", documentNumber);
        
        List<CaseInstanceDocument> caseInstances = caseInstanceRepository.findByDocumentNumber(documentNumber);
        
        if (caseInstances.isEmpty()) {
            log.info("Case not found in MongoDB for document number: {} - treating as not in progress", documentNumber);
            return CaseAnalysisResult.builder()
                    .found(false)
                    .caseStatus(null)
                    .isInProgress(false)
                    .isStale(false)
                    .lastUpdated(null)
                    .multipleResults(false)
                    .resultCount(0)
                    .build();
        }
        
        // Check if there are multiple results
//        if (caseInstances.size() > 1) {
//            log.warn("Multiple case instances ({}) found for document number: {} - flagging for manual review",
//                    caseInstances.size(), documentNumber);
//
//            // Get the most recently updated document for status information
//            CaseInstanceDocument latestDocument = caseInstances.stream()
//                    .max((d1, d2) -> {
//                        LocalDateTime t1 = d1.getUpdatedAt() != null ? d1.getUpdatedAt() : d1.getCreatedAt();
//                        LocalDateTime t2 = d2.getUpdatedAt() != null ? d2.getUpdatedAt() : d2.getCreatedAt();
//                        if (t1 == null) return -1;
//                        if (t2 == null) return 1;
//                        return t1.compareTo(t2);
//                    })
//                    .orElse(caseInstances.get(0));
//
//            return CaseAnalysisResult.builder()
//                    .found(true)
//                    .caseStatus(latestDocument.getCaseStatus())
//                    .statusField(latestDocument.getStatus())
//                    .isInProgress(false) // Don't process automatically
//                    .isStale(false)
//                    .lastUpdated(latestDocument.getUpdatedAt())
//                    .createdDate(latestDocument.getCreatedAt())
//                    .multipleResults(true)
//                    .resultCount(caseInstances.size())
//                    .requiresManualReview(true)
//                    .manualReviewReason("Multiple case instances found (" + caseInstances.size() + " results)")
//                    .build();
//        }

        CaseInstanceDocument document = null;
        for (CaseInstanceDocument caseInstanceDocument : caseInstances) {
            boolean isFollowUpTaskPresent = caseInstanceDocument.getTasks().stream()
                    .filter(Objects::nonNull)
                    .anyMatch(caseTask -> caseTask.getTaskName() != null &&
                            caseTask.getTaskType().equalsIgnoreCase("BPM Follow-Up"));
            if (isFollowUpTaskPresent) {
                boolean doesTaskIdMatch = caseInstanceDocument.getTasks().stream()
                        .filter(Objects::nonNull)
                        .filter(caseTask -> caseTask.getTaskName() != null &&
                                caseTask.getTaskType().equalsIgnoreCase("BPM Follow-Up"))
                        .anyMatch(caseTask -> caseTask.getId() != null &&
                                onbaseTaskId.equalsIgnoreCase(caseTask.getId()));
                if (doesTaskIdMatch) {
                    document = caseInstanceDocument;
                    break;
                }
            }
        }

        if (Objects.isNull(document)) {
            log.info("Case not found in MongoDB for document number: {} - treating as not in progress", documentNumber);
            return CaseAnalysisResult.builder()
                    .found(false)
                    .caseStatus(null)
                    .isInProgress(false)
                    .isStale(false)
                    .lastUpdated(null)
                    .multipleResults(false)
                    .resultCount(0)
                    .build();
        }
//        CaseInstanceDocument document = caseInstances.get(0);
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
        boolean isDeTaskPresent = true;
        boolean isDeTaskComplete = false;
        LocalDateTime lastUpdated = document.getUpdatedAt();
        
        if (isInProgress) {
            if (document.getTasks() != null && !document.getTasks().isEmpty()) {
                isDeTaskPresent = document.getTasks().stream()
                        .filter(Objects::nonNull)
                        .anyMatch(caseTask -> caseTask.getTaskName() != null && 
                                caseTask.getTaskName().equalsIgnoreCase("Data entry"));

                isDeTaskComplete = document.getTasks().stream()
                        .filter(Objects::nonNull)
                        .filter(caseTask -> caseTask.getTaskName() != null && 
                                caseTask.getTaskName().equalsIgnoreCase("Data entry"))
                        .allMatch(caseTask -> caseTask.getStatus() != null && 
                                "COMPLETED".equalsIgnoreCase(caseTask.getStatus()));
            } else {
                isDeTaskPresent = false;
            }

            if (lastUpdated == null) {
                lastUpdated = document.getCreatedAt();
            }
            
            if (lastUpdated != null) {
                // Use business days calculation (excluding weekends)
                isStale = usBusinessDayService.isDateDifferTwo(lastUpdated.toLocalDate(), 2);
            }
        }
        
        log.info("Case analysis - caseStatus: {}, status: {}, isInProgress: {}, isStale: {}, lastUpdated: {}", 
                caseStatus, document.getStatus(), isInProgress, isStale, lastUpdated);
        
        return CaseAnalysisResult.builder()
                .found(true)
                .caseStatus(caseStatus)
                .statusField(document.getStatus())
                .isInProgress(isInProgress)
                .isDeTaskPresent(isDeTaskPresent)
                .isDeTaskComplete(isDeTaskComplete)
                .isStale(isStale)
                .lastUpdated(lastUpdated)
                .createdDate(document.getCreatedAt())
                .multipleResults(false)
                .resultCount(1)
                .requiresManualReview(false)
                .manualReviewReason(null)
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
        private boolean isInProgress;
        private boolean isDeTaskPresent;
        private boolean isDeTaskComplete;// Is the case IN_PROGRESS?
        private boolean isStale;            // Is it older than threshold?
        private LocalDateTime lastUpdated;  // Last updated timestamp
        private LocalDateTime createdDate;  // Created timestamp
        private boolean multipleResults;    // Were multiple results found?
        private int resultCount;            // Number of results found
        private boolean requiresManualReview; // Should this be manually reviewed?
        private String manualReviewReason;  // Why manual review is needed
    }
    


}

