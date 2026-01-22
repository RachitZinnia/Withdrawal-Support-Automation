package com.withdrawal.support.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.withdrawal.support.config.BusinessConfig;
import com.withdrawal.support.dto.*;
import com.withdrawal.support.model.CaseCategory;
import com.withdrawal.support.model.CaseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaseProcessingService {

    private final DataEntryService dataEntryService;
    private final OnBaseService onBaseService;
    private final CaseMongoService caseMongoService;
    private final BusinessConfig businessConfig;

    /**
     * Main method to process all data entry waiting cases
     * This orchestrates the entire workflow described in requirements
     */
    public ProcessingResult processDataEntryWaitingCases() {
        log.info("Starting data entry waiting cases processing");
        
        ProcessingResult result = ProcessingResult.builder()
                .details(new ArrayList<>())
                .documentNumbersToCancel(new ArrayList<>())
                .documentNumbersToReturning(new ArrayList<>())
                .documentNumbersToComplete(new ArrayList<>())
                .documentNumbersForManualReview(new ArrayList<>())
                .documentNumbersToRetriggerEvent(new ArrayList<>())
                .build();

        try {
            // Step 1: Get all data entry waiting cases
//             DataEntryCase deCase = new DataEntryCase("1","264c60de-e6a4-11f0-a84d-025eefd42701",false,"1");
//             List<DataEntryCase> waitingCases = new ArrayList<>(List.of(deCase));
            List<DataEntryCase> waitingCases = dataEntryService.getDataEntryWaitingCases();
            result.setTotalCases(waitingCases.size());
            log.info("Found {} waiting cases to process", waitingCases.size());

            // Step 2-6: Process each case
            for (DataEntryCase dataEntryCase : waitingCases) {
                try {
                    CaseProcessingDetail detail = processSingleCase(dataEntryCase, result);
                    result.getDetails().add(detail);
                    
                    if (detail.isRequiresManualReview()) {
                        result.setManualReviewRequired(result.getManualReviewRequired() + 1);
                    } else if (detail.getStatus() == CaseStatus.COMPLETED) {
                        result.setSuccessfulCases(result.getSuccessfulCases() + 1);
                    } else if (detail.getStatus() == CaseStatus.FAILED) {
                        result.setFailedCases(result.getFailedCases() + 1);
                    }
                    
                } catch (Exception e) {
                    log.error("Error processing case: {}", dataEntryCase.getProcessInstanceId(), e);
                    result.setFailedCases(result.getFailedCases() + 1);
                    result.getDetails().add(CaseProcessingDetail.builder()
                            .caseReference(dataEntryCase.getProcessInstanceId())
                            .status(CaseStatus.MANUAL_REVIEW_REQUIRED)
                            .message("Error: " + e.getMessage() + " manual review").requiresManualReview(true).reviewReason("Exception")
                            .processedAt(LocalDateTime.now())
                            .build());
                }
            }


            result.setMessage(String.format(
                    "Processing completed: %d total, %d successful, %d failed, %d require manual review. " +
                    "To cancel: %d, To returning: %d, To complete: %d, Manual review: %d, To retrigger event: %d",
                    result.getTotalCases(),
                    result.getSuccessfulCases(),
                    result.getFailedCases(),
                    result.getManualReviewRequired(),
                    result.getDocumentNumbersToCancel().size(),
                    result.getDocumentNumbersToReturning().size(),
                    result.getDocumentNumbersToComplete().size(),
                    result.getDocumentNumbersForManualReview().size(),
                    result.getDocumentNumbersToRetriggerEvent().size()
            ));
            
            log.info("Processing completed successfully: {}", result.getMessage());
            log.info("Document numbers to cancel: {}", result.getDocumentNumbersToCancel());
            log.info("Document numbers to returning: {}", result.getDocumentNumbersToReturning());
            log.info("Document numbers to complete: {}", result.getDocumentNumbersToComplete());
            log.info("Document numbers for manual review: {}", result.getDocumentNumbersForManualReview());
            log.info("Document numbers to retrigger event: {}", result.getDocumentNumbersToRetriggerEvent());
            
        } catch (Exception e) {
            log.error("Error during case processing", e);
            result.setMessage("Processing failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * Processes a single case through the entire workflow
     */
    private CaseProcessingDetail processSingleCase(DataEntryCase dataEntryCase, ProcessingResult result) {
        log.info("Processing case with process instance ID: {}", dataEntryCase.getProcessInstanceId());
        
        CaseProcessingDetail detail = CaseProcessingDetail.builder()
                .caseReference(dataEntryCase.getProcessInstanceId())
                .processedAt(LocalDateTime.now())
                .build();
        String documentNumber = "";
        try {
            // Step 2: Get case details from Camunda (clientCode and onbaseCaseId)
            CaseDetails caseDetails = dataEntryService.getCaseDetailsForWithdrawals(dataEntryCase.getProcessInstanceId());
            detail.setCaseId(caseDetails.getCaseId());
            detail.setClientCode(caseDetails.getClientCode());

            // Step 3: Get case information from OnBase
            OnBaseCaseDetails onBaseDetails = onBaseService.getOnBaseCaseDetails(
                    caseDetails.getClientCode(), 
                    caseDetails.getCaseId()
            );

            detail.setOnbaseStatus(onBaseDetails.getStatus());
            documentNumber = onBaseDetails.getDocumentNumber();
            detail.setDocumentNumber(documentNumber);
            
            // Get BPM Follow-Up status and set it on the detail
            OnBaseService.BpmFollowUpStatus bpmStatus = onBaseService.getBpmFollowUpStatus(onBaseDetails);
            detail.setBpmFollowUpStatus(bpmStatus.getStatusText());
            detail.setBpmFollowUpTotal(bpmStatus.getTotal());
            detail.setBpmFollowUpOpen(bpmStatus.getOpen());

            // Step 4: Categorize case based on status and BPM Follow-Up tasks
            CaseCategory category = onBaseService.categorizeCaseByStatus(onBaseDetails);
            detail.setCategory(category);
            
            String categoryDescription = onBaseService.getCategoryDescription(category);
            detail.setAction(category.name());

            // Step 5: Process based on category
            switch (category) {
                case FOLLOW_UP_COMPLETE -> {
                    // All BPM Follow-Up tasks complete - will cancel
                    log.info("Case {} - All BPM Follow-Up complete, marking for cancellation", caseDetails.getCaseId());
                    detail.setStatus(CaseStatus.COMPLETED);
                    detail.setMessage(categoryDescription + " - Will cancel");
                    
                    // Add to cancellation list
                    if (documentNumber != null && !result.getDocumentNumbersToCancel().contains(documentNumber)) {
                        result.getDocumentNumbersToCancel().add(documentNumber);
                        log.info("Added document {} to cancellation list (FOLLOW_UP_COMPLETE)", documentNumber);
                    }
                }
                
                case DV_POST_OPEN_DV_COMPLETE -> {
                    // Status "Post Complete" but BPM Follow-Up not complete - will cancel
                    log.info("Case {} - Post Complete with incomplete BPM Follow-Up, marking for cancellation", 
                            caseDetails.getCaseId());
                    detail.setStatus(CaseStatus.COMPLETED);
                    detail.setMessage(categoryDescription + " - Will cancel");
                    
                    // Add to cancellation list
                    if (documentNumber != null && !result.getDocumentNumbersToCancel().contains(documentNumber)) {
                        result.getDocumentNumbersToCancel().add(documentNumber);
                        log.info("Added document {} to cancellation list (DV_POST_OPEN_DV_COMPLETE)", documentNumber);
                    }
                    if (documentNumber != null && !result.getDocumentNumbersToComplete().contains(documentNumber)) {
                        result.getDocumentNumbersToComplete().add(documentNumber);
                        log.info("Added document {} to complete list (DV_POST_OPEN_DV_COMPLETE)", documentNumber);
                    }
                }
                
                case CHECK_MONGODB -> {
                    // Status Pend/Pending/New with BPM Follow-Up not complete - check MongoDB
                    log.info("Case {} - Pend/Pending/New status, checking MongoDB with document number: {}", 
                            caseDetails.getCaseId(), documentNumber);

                    String dataEntryOnBaseTaskID = dataEntryService.getCamundaVariable(dataEntryCase.getProcessInstanceId(), "dataEntryOnBaseTaskID");

                    // Query MongoDB ONCE and get all information
                    CaseMongoService.CaseAnalysisResult mongoAnalysis = caseMongoService.analyzeCaseFromMongo(
                            documentNumber, 
                            businessConfig.getDaysThreshold(),
                            dataEntryOnBaseTaskID
                    );
                    
                    // Check if multiple results were found - requires manual review
                    if (!mongoAnalysis.isDeTaskPresent()) {
                        log.warn("Document {} - Data Entry Task not present ({}), flagging for manual review",
                                documentNumber, mongoAnalysis.getResultCount());
                        detail.setStatus(CaseStatus.MANUAL_REVIEW_REQUIRED);
                        detail.setMessage(categoryDescription + " - " + "Data Entry Task not present");
                        detail.setRequiresManualReview(true);
                        detail.setReviewReason("Data Entry Task not present");
                        
                        // Add to manual review list
                        if (documentNumber != null && !result.getDocumentNumbersForManualReview().contains(documentNumber)) {
                            result.getDocumentNumbersForManualReview().add(documentNumber);
                            log.info("Added document {} to manual review list (Data Entry Task not present)", documentNumber);
                        }
                    } else if (mongoAnalysis.isDeTaskComplete()) {
                        log.info("Document {} - Data Entry Task status complete in CM ({}), adding to retrigger event list",
                                documentNumber, mongoAnalysis.getResultCount());
                        detail.setStatus(CaseStatus.MANUAL_REVIEW_REQUIRED);
                        detail.setMessage(categoryDescription + " - " + "Data Entry Task status complete in CM - Will retrigger event");
                        detail.setRequiresManualReview(true);

                        // Add to retrigger event list
                        if (documentNumber != null && !result.getDocumentNumbersToRetriggerEvent().contains(documentNumber)) {
                            result.getDocumentNumbersToRetriggerEvent().add(documentNumber);
                            log.info("Added document {} to retrigger event list (Data Entry Task status complete in CM)", documentNumber);
                        }
                    } else if (!mongoAnalysis.isInProgress()) {

                        if (mongoAnalysis.getCaseStatus().equalsIgnoreCase("COMPLETE")) {
                            // caseStatus is NOT IN_PROGRESS - add to cancellation list
                            log.info("Document {} - caseStatus '{}' is NOT IN_PROGRESS, marking for cancellation",
                                    documentNumber, mongoAnalysis.getCaseStatus());
                            detail.setStatus(CaseStatus.COMPLETED);
                            detail.setMessage(categoryDescription +
                                    " - MongoDB caseStatus: " + mongoAnalysis.getCaseStatus() + " (is Complete) - Will cancel");

                            // Add to cancellation list
                            if (documentNumber != null && !result.getDocumentNumbersToCancel().contains(documentNumber)) {
                                result.getDocumentNumbersToCancel().add(documentNumber);
                                log.info("Added document {} to cancellation list (MongoDB NOT IN_PROGRESS)", documentNumber);
                            }
                            if (documentNumber != null && !result.getDocumentNumbersToReturning().contains(documentNumber)) {
                                result.getDocumentNumbersToReturning().add(documentNumber);
                                log.info("Added document {} to returning list (MongoDB NOT IN_PROGRESS)", documentNumber);
                            }
                        } else if (mongoAnalysis.getCaseStatus().equalsIgnoreCase("EXCEPTION")) {
                            // caseStatus is NOT IN_PROGRESS - add to cancellation list
                            log.info("Document {} - caseStatus '{}' is Exception, marking for cancel",
                                    documentNumber, mongoAnalysis.getCaseStatus());
                            detail.setStatus(CaseStatus.EXCEPTION);
                            detail.setMessage(categoryDescription +
                                    " - MongoDB caseStatus: " + mongoAnalysis.getCaseStatus() + " (is exception) - Will cancel");

                            // Add to cancellation list
                            if (documentNumber != null && !result.getDocumentNumbersToCancel().contains(documentNumber)) {
                                result.getDocumentNumbersToCancel().add(documentNumber);
                                log.info("Added document {} to cancellation list (MongoDB in exception)", documentNumber);
                            }
                            if (documentNumber != null && !result.getDocumentNumbersToReturning().contains(documentNumber)) {
                                result.getDocumentNumbersToReturning().add(documentNumber);
                                log.info("Added document {} to returning list (MongoDB in exception)", documentNumber);
                            }
                        }

 
                    } else {
                        // caseStatus IS IN_PROGRESS - check if stale
                        log.info("Document {} is IN_PROGRESS in MongoDB (last updated: {})", 
                                documentNumber, mongoAnalysis.getLastUpdated());
                        
                        if (mongoAnalysis.isStale()) {
                            // IN_PROGRESS but older than threshold - manual review
                            log.warn("Document {} is IN_PROGRESS but stale (>{} business days), flagging for manual review", 
                                    documentNumber, businessConfig.getDaysThreshold());
                            detail.setStatus(CaseStatus.MANUAL_REVIEW_REQUIRED);
                            detail.setMessage(categoryDescription + 
                                    " - IN_PROGRESS but stale (>" + businessConfig.getDaysThreshold() + " days) - Manual review required");
                            detail.setRequiresManualReview(true);
                            detail.setReviewReason("IN_PROGRESS for more than " + businessConfig.getDaysThreshold() + " business days");
                            
                            // Add to manual review list
                            if (documentNumber != null && !result.getDocumentNumbersForManualReview().contains(documentNumber)) {
                                result.getDocumentNumbersForManualReview().add(documentNumber);
                                log.info("Added document {} to manual review list (stale IN_PROGRESS)", documentNumber);
                            }
                        } else {
                            // IN_PROGRESS and recent - continue monitoring
                            log.info("Document {} is IN_PROGRESS and recent, continue monitoring", documentNumber);
                            detail.setStatus(CaseStatus.IN_PROGRESS);
                            detail.setMessage(categoryDescription + " - IN_PROGRESS in MongoDB - Continue monitoring");
                        }
                    }
                }
                
                default -> {
                    // Unknown category - requires manual review
                    log.warn("Case {} - Unknown category, marking for manual review", caseDetails.getCaseId());
                    detail.setStatus(CaseStatus.MANUAL_REVIEW_REQUIRED);
                    detail.setMessage(categoryDescription);
                    detail.setRequiresManualReview(true);
                    detail.setReviewReason("Unknown category");
                    
                    // Add to manual review list
                    if (documentNumber != null && !result.getDocumentNumbersForManualReview().contains(documentNumber)) {
                        result.getDocumentNumbersForManualReview().add(documentNumber);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error processing case: {}", dataEntryCase.getProcessInstanceId(), e);
            documentNumber = dataEntryService.getCamundaVariable(dataEntryCase.getProcessInstanceId(), "documentNumber");
            detail.setStatus(CaseStatus.MANUAL_REVIEW_REQUIRED);
            detail.setMessage("Error: " + e.getMessage() + " Requires manual review");
            detail.setRequiresManualReview(true);
            detail.setReviewReason("Exception");
            if (documentNumber != null && !result.getDocumentNumbersForManualReview().contains(documentNumber)) {
                result.getDocumentNumbersForManualReview().add(documentNumber);
            }
        }

        return detail;
    }

}

