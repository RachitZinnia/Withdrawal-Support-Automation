package com.withdrawal.support.service;

import com.withdrawal.support.config.BusinessConfig;
import com.withdrawal.support.dto.*;
import com.withdrawal.support.model.CaseCategory;
import com.withdrawal.support.model.CaseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyReportProcessingService {

    private final CsvProcessingService csvProcessingService;
    private final DataEntryService dataEntryService;
    private final OnBaseService onBaseService;
    private final CaseMongoService caseMongoService;
    private final BusinessConfig businessConfig;

    /**
     * Main method to process daily report CSV file
     */
    public DailyReportProcessingResult processDailyReport(MultipartFile csvFile) {
        log.info("Starting daily report processing for file: {}", csvFile.getOriginalFilename());
        
        DailyReportProcessingResult result = DailyReportProcessingResult.builder()
                .details(new ArrayList<>())
                .businessKeysExtracted(new ArrayList<>())
                .documentNumbersToCancel(new ArrayList<>())
                .documentNumbersToReturning(new ArrayList<>())
                .documentNumbersToComplete(new ArrayList<>())
                .documentNumbersForManualReview(new ArrayList<>())
                .documentNumbersWithActiveInstances(new ArrayList<>())
                .build();

        try {
            // Step 1: Parse CSV file
            List<DailyReportRow> allRows = csvProcessingService.parseCsvFile(csvFile);
            result.setTotalRowsInCsv(allRows.size());
            log.info("Parsed {} total rows from CSV", allRows.size());

            // Step 2: Filter for "Not Matching" rows
            List<DailyReportRow> notMatchingRows = csvProcessingService.filterNotMatchingRows(allRows);
            result.setNotMatchingRows(notMatchingRows.size());
            log.info("Found {} 'Not Matching' rows", notMatchingRows.size());

            // Step 3: Extract business keys
            List<String> businessKeys = csvProcessingService.extractBusinessKeys(notMatchingRows);
//            List<String> businessKeys = new ArrayList<>();
//            businessKeys.add("20251217-EM-106066");
//            List<String> businessKeys = List.of("20251208-W-608306");
            result.setBusinessKeysExtracted(businessKeys);
            log.info("Extracted {} unique business keys", businessKeys.size());

            // Step 4: Process each business key
            for (String businessKey : businessKeys) {
                try {
                    List<CaseProcessingDetail> caseDetails = processBusinessKey(businessKey, result);
                    result.getDetails().addAll(caseDetails);
                    result.setProcessedCases(result.getProcessedCases() + caseDetails.size());
                    
                    // Update counters
                    for (CaseProcessingDetail detail : caseDetails) {
                        if (detail.isRequiresManualReview()) {
                            result.setManualReviewRequired(result.getManualReviewRequired() + 1);
                        } else if (detail.getStatus() == CaseStatus.COMPLETED) {
                            result.setSuccessfulCases(result.getSuccessfulCases() + 1);
                        } else if (detail.getStatus() == CaseStatus.FAILED) {
                            result.setFailedCases(result.getFailedCases() + 1);
                        }
                    }
                    
                } catch (Exception e) {
                    log.error("Error processing business key: {}", businessKey, e);
                    result.setFailedCases(result.getFailedCases() + 1);
                    
                    // Add business key to manual review list due to exception
                    if (!result.getDocumentNumbersForManualReview().contains(businessKey)) {
                        result.getDocumentNumbersForManualReview().add(businessKey);
                        log.info("Added business key {} to manual review list due to exception: {}", 
                                businessKey, e.getMessage());
                    }
                }
            }

            result.setMessage(String.format(
                    "Daily Report Processing: %d total rows, %d not matching, %d business keys, %d cases processed. " +
                    "To cancel: %d, To returning: %d, To complete: %d, Manual review: %d",
                    result.getTotalRowsInCsv(),
                    result.getNotMatchingRows(),
                    result.getBusinessKeysExtracted().size(),
                    result.getProcessedCases(),
                    result.getDocumentNumbersToCancel().size(),
                    result.getDocumentNumbersToReturning().size(),
                    result.getDocumentNumbersToComplete().size(),
                    result.getDocumentNumbersForManualReview().size()
            ));
            
            log.info("Daily report processing completed: {}", result.getMessage());
            
        } catch (Exception e) {
            log.error("Error during daily report processing", e);
            result.setMessage("Processing failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * Processes a single business key:
     * 1. Get process instance IDs from business key
     * 2. For each process instance, get clientCode and onbaseCaseId
     * 3. Call OnBase API
     * 4. Process similar to data entry waiting cases
     */
    private List<CaseProcessingDetail> processBusinessKey(String businessKey, DailyReportProcessingResult result) {
        log.info("Processing business key: {}", businessKey);
        
        List<CaseProcessingDetail> caseDetails = new ArrayList<>();
        
        try {
            // Step 1: Get process instance IDs for this business key
            DataEntryService.ProcessInstanceResult processResult = getProcessInstanceIdsFromBusinessKey(businessKey);
            List<String> processInstanceIds = processResult.getProcessInstanceIds();
            boolean hasActiveInstances = processResult.isHasActiveInstances();
            
            log.info("Found {} process instances for business key: {} (hasActive: {})", 
                    processInstanceIds.size(), businessKey, hasActiveInstances);
            
            // Step 2: Process each process instance
            for (String processInstanceId : processInstanceIds) {
                try {
                    CaseProcessingDetail detail = processProcessInstanceFromBusinessKey(
                            processInstanceId, result, hasActiveInstances);
                    caseDetails.add(detail);
                } catch (Exception e) {
                    log.error("Error processing process instance {}: {}", processInstanceId, e.getMessage());
                    caseDetails.add(CaseProcessingDetail.builder()
                            .caseReference(processInstanceId)
                            .status(CaseStatus.FAILED)
                            .message("Error: " + e.getMessage())
                            .processedAt(LocalDateTime.now())
                            .build());
                }
            }
            
        } catch (Exception e) {
            log.error("Error processing business key {}: {}", businessKey, e.getMessage());
        }
        
        return caseDetails;
    }

    /**
     * Gets process instance IDs from a business key
     * Calls Camunda API: /history/process-instance?processInstanceBusinessKey={businessKey}
     */
    private DataEntryService.ProcessInstanceResult getProcessInstanceIdsFromBusinessKey(String businessKey) {
        log.info("Fetching process instance IDs for business key: {}", businessKey);
        return dataEntryService.getProcessInstanceIdsByBusinessKey(businessKey,"");
    }

    /**
     * Processes a single process instance from business key
     * Similar logic to data entry waiting cases
     */
    private CaseProcessingDetail processProcessInstanceFromBusinessKey(String processInstanceId, 
                                                                       DailyReportProcessingResult result, 
                                                                       boolean hasActiveInstances) {
        log.info("Processing process instance from business key: {} (hasActiveInstances: {})", 
                processInstanceId, hasActiveInstances);
        
        CaseProcessingDetail detail = CaseProcessingDetail.builder()
                .caseReference(processInstanceId)
                .processedAt(LocalDateTime.now())
                .build();

        try {
            // Step 1: Get case details from Camunda (clientCode and onbaseCaseId)
            CaseDetails caseDetails = dataEntryService.getCaseDetails(processInstanceId);
            detail.setCaseId(caseDetails.getCaseId());
            detail.setClientCode(caseDetails.getClientCode());

            // Step 2: Get case information from OnBase
            OnBaseCaseDetails onBaseDetails = onBaseService.getOnBaseCaseDetails(
                    caseDetails.getClientCode(), 
                    caseDetails.getCaseId()
            );

            detail.setOnbaseStatus(onBaseDetails.getStatus());
            String documentNumber = onBaseDetails.getDocumentNumber();
            detail.setDocumentNumber(documentNumber);
            
            // Get BPM Follow-Up status and set it on the detail
            OnBaseService.BpmFollowUpStatus bpmStatus = onBaseService.getBpmFollowUpStatus(onBaseDetails);
            detail.setBpmFollowUpStatus(bpmStatus.getStatusText());
            detail.setBpmFollowUpTotal(bpmStatus.getTotal());
            detail.setBpmFollowUpOpen(bpmStatus.getOpen());

            // Step 3: Categorize case based on status and BPM Follow-Up tasks
            CaseCategory category = onBaseService.categorizeCaseByStatus(onBaseDetails);
            
            // If there are active instances, override category to UNKNOWN
            if (hasActiveInstances) {
                log.info("Active instances found - setting category to UNKNOWN for manual review");
                category = CaseCategory.WAITING_CASE;
            }
            
            detail.setCategory(category);
            String categoryDescription = onBaseService.getCategoryDescription(category);
            detail.setAction(category.name());

            // Step 4: Process based on category (similar to data entry waiting cases)
            processCaseByCategory(category, categoryDescription, caseDetails, onBaseDetails, 
                                 documentNumber, detail, result);

        } catch (Exception e) {
            log.error("Error processing process instance: {}", processInstanceId, e);
            detail.setStatus(CaseStatus.FAILED);
            detail.setMessage("Error: " + e.getMessage());
        }

        return detail;
    }

    /**
     * Processes case based on category (extracted to avoid code duplication)
     */
    private void processCaseByCategory(CaseCategory category, String categoryDescription,
                                      CaseDetails caseDetails, OnBaseCaseDetails onBaseDetails,
                                      String documentNumber, CaseProcessingDetail detail,
                                      DailyReportProcessingResult result) {
        
        switch (category) {
            case WAITING_CASE -> {
                log.info("active process instance present marking it in active case");
                detail.setStatus(CaseStatus.ACTIVE_CASE);
                detail.setMessage(categoryDescription);

                if (documentNumber !=null && !result.getDocumentNumbersWithActiveInstances().contains(documentNumber)){
                    result.getDocumentNumbersWithActiveInstances().add(documentNumber);
                    log.info("Added document {} to active instance list", documentNumber);
                }
            }
            case FOLLOW_UP_COMPLETE -> {
                log.info("Case {} - All BPM Follow-Up complete, marking for manual review", caseDetails.getCaseId());
                detail.setStatus(CaseStatus.NO_ACTION_REQUIRED);
                detail.setMessage(categoryDescription + " - No Action Required");

            }
            
            case DV_POST_OPEN_DV_COMPLETE -> {
                log.info("Case {} - Post Complete with incomplete BPM Follow-Up", caseDetails.getCaseId());
                detail.setStatus(CaseStatus.COMPLETED);
                detail.setMessage(categoryDescription + " - Will cancel");

                if (documentNumber != null && !result.getDocumentNumbersToComplete().contains(documentNumber)) {
                    result.getDocumentNumbersToComplete().add(documentNumber);
                }
            }
            
            case CASE_RETURNING -> {
                log.info("Case {} - adding to CP Returing queue since no active instance found and bpm follow up open for document: {}", caseDetails.getCaseId(), documentNumber);

                if (documentNumber != null && !result.getDocumentNumbersToReturning().contains(documentNumber)) {
                    result.getDocumentNumbersToReturning().add(documentNumber);
                }
            }
            
            default -> {
                log.warn("Case {} - Unknown category", caseDetails.getCaseId());
                detail.setStatus(CaseStatus.MANUAL_REVIEW_REQUIRED);
                detail.setMessage(categoryDescription);
                detail.setRequiresManualReview(true);
                detail.setReviewReason("Unknown category");
                
                if (documentNumber != null && !result.getDocumentNumbersForManualReview().contains(documentNumber)) {
                    result.getDocumentNumbersForManualReview().add(documentNumber);
                }
            }
        }
    }
}

