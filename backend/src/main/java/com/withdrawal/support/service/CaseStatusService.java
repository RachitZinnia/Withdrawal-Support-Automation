package com.withdrawal.support.service;

import com.withdrawal.support.dto.CaseStatusResponse;
import com.withdrawal.support.dto.OnBaseCaseDetails;
import com.withdrawal.support.model.CaseInstanceDocument;
import com.withdrawal.support.repository.CaseInstanceRepository;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CaseStatusService {

    private static final Logger log = LoggerFactory.getLogger(CaseStatusService.class);
    
    @Autowired
    OnBaseService onBaseService;

    @Autowired
    DataEntryService dataEntryService;

    @Autowired
    CaseInstanceRepository caseInstanceRepository;

    /**
     * Close BPM Follow-Up tasks for the given document numbers
     */
    public CaseStatusResponse closeBpmnFollowUpTask(List<String> documentNumberList) {
        List<String> successfulDocuments = new ArrayList<>();
        List<CaseStatusResponse.FailedDocument> failedDocuments = new ArrayList<>();
        List<CaseStatusResponse.CreateOscDocument> createOscDocuments = new ArrayList<>();

        for (String documentNumber : documentNumberList) {
            try {
                processCloseBpmFollowUp(documentNumber, null, successfulDocuments, failedDocuments, createOscDocuments);
            } catch (Exception e) {
                log.error("Unexpected error processing document {}: {}", documentNumber, e.getMessage(), e);
                createOscDocuments.add(CaseStatusResponse.CreateOscDocument.builder()
                        .documentNumber(documentNumber)
                        .reason("Unexpected error: " + e.getMessage())
                        .oscType("Close BPM Follow-Up")
                        .build());
            }
        }

        return buildResponse(documentNumberList.size(), successfulDocuments, failedDocuments, createOscDocuments, 
                "BPM Follow-Up task closing completed");
    }

    /**
     * Move cases to CP Returning queue
     */
    public CaseStatusResponse moveCaseToCpReturning(List<String> documentNumberList) {
        List<String> successfulDocuments = new ArrayList<>();
        List<CaseStatusResponse.FailedDocument> failedDocuments = new ArrayList<>();
        List<CaseStatusResponse.CreateOscDocument> createOscDocuments = new ArrayList<>();

        for (String documentNumber : documentNumberList) {
            try {
                processCloseBpmFollowUp(documentNumber, "CP - Returning", successfulDocuments, failedDocuments, createOscDocuments);
            } catch (Exception e) {
                log.error("Unexpected error processing document {}: {}", documentNumber, e.getMessage(), e);
                createOscDocuments.add(CaseStatusResponse.CreateOscDocument.builder()
                        .documentNumber(documentNumber)
                        .reason("Unexpected error: " + e.getMessage())
                        .oscType("Move to CP Returning")
                        .build());
            }
        }

        return buildResponse(documentNumberList.size(), successfulDocuments, failedDocuments, createOscDocuments,
                "Move to CP Returning completed");
    }

    /**
     * Move cases to DV Post Complete queue
     */
    public CaseStatusResponse moveCaseToDvPostComplete(List<String> documentNumberList) {
        List<String> successfulDocuments = new ArrayList<>();
        List<CaseStatusResponse.FailedDocument> failedDocuments = new ArrayList<>();
        List<CaseStatusResponse.CreateOscDocument> createOscDocuments = new ArrayList<>();

        for (String documentNumber : documentNumberList) {
            try {
                processCloseBpmFollowUp(documentNumber, "CP - BPM Complete", successfulDocuments, failedDocuments, createOscDocuments);
            } catch (Exception e) {
                log.error("Unexpected error processing document {}: {}", documentNumber, e.getMessage(), e);
                createOscDocuments.add(CaseStatusResponse.CreateOscDocument.builder()
                        .documentNumber(documentNumber)
                        .reason("Unexpected error: " + e.getMessage())
                        .oscType("Move to DV Post Complete")
                        .build());
            }
        }

        return buildResponse(documentNumberList.size(), successfulDocuments, failedDocuments, createOscDocuments,
                "Move to DV Post Complete completed");
    }

    /**
     * Process a single document for closing BPM follow-up and optionally moving to a queue
     */
    private void processCloseBpmFollowUp(String documentNumber, String queueName,
                                          List<String> successfulDocuments,
                                          List<CaseStatusResponse.FailedDocument> failedDocuments,
                                          List<CaseStatusResponse.CreateOscDocument> createOscDocuments) {
        
        log.info("Processing document: {}, queueName: {}", documentNumber, queueName);
        
        try {
            DataEntryService.ProcessInstanceResult processInstanceResult = 
                    dataEntryService.getProcessInstanceIdsByBusinessKey(documentNumber, "");
            
            if (processInstanceResult.getProcessInstanceIds().isEmpty()) {
                // No process instance found - add to create OSC list
                log.warn("No process instance found for document: {}", documentNumber);
                String oscType = queueName == null ? "Close BPM Follow-Up" : 
                        (queueName.contains("Returning") ? "Move to CP Returning" : "Move to DV Post Complete");
                createOscDocuments.add(CaseStatusResponse.CreateOscDocument.builder()
                        .documentNumber(documentNumber)
                        .reason("No process instance found")
                        .oscType(oscType)
                        .build());
                return;
            }

            String processInstance = processInstanceResult.getProcessInstanceIds().get(0);
            
            String clientCode;
            String onbaseCaseId;
            
            try {
                clientCode = dataEntryService.getCamundaVariable(processInstance, "clientCode");
                onbaseCaseId = dataEntryService.getCamundaVariable(processInstance, "onbaseCaseId");
            } catch (Exception e) {
                log.error("Failed to get Camunda variables for document {}: {}", documentNumber, e.getMessage());
                failedDocuments.add(CaseStatusResponse.FailedDocument.builder()
                        .documentNumber(documentNumber)
                        .reason("Failed to get Camunda variables: " + e.getMessage())
                        .build());
                return;
            }

            OnBaseCaseDetails onBaseCaseDetails;
            try {
                onBaseCaseDetails = onBaseService.getOnBaseCaseDetails(clientCode, onbaseCaseId);
            } catch (Exception e) {
                log.error("Failed to get OnBase case details for document {}: {}", documentNumber, e.getMessage());
                String oscType = queueName == null ? "Close BPM Follow-Up" : 
                        (queueName.contains("Returning") ? "Move to CP Returning" : "Move to DV Post Complete");
                createOscDocuments.add(CaseStatusResponse.CreateOscDocument.builder()
                        .documentNumber(documentNumber)
                        .reason("Failed to get OnBase case details: " + e.getMessage())
                        .oscType(oscType)
                        .build());
                return;
            }

            if (onBaseCaseDetails == null || onBaseCaseDetails.getTasks() == null) {
                log.warn("No tasks found for document: {}", documentNumber);
                failedDocuments.add(CaseStatusResponse.FailedDocument.builder()
                        .documentNumber(documentNumber)
                        .reason("No tasks found in OnBase case")
                        .build());
                return;
            }

            boolean isFollowupOpen = false;
            boolean taskCloseFailed = false;
            
            for (OnBaseCaseDetails.Task task : onBaseCaseDetails.getTasks()) {
                if ("BPM Follow-Up".equalsIgnoreCase(task.getTaskType()) && !"Complete".equalsIgnoreCase(task.getStatus())) {
                    isFollowupOpen = true;
                    try {
                        onBaseService.manageOnbaseTask(task.getTaskID().toString(), clientCode, "tp - exit {admin}");
                        log.info("Successfully closed BPM Follow-Up task {} for document {}", task.getTaskID(), documentNumber);
                    } catch (Exception e) {
                        log.error("Failed to close BPM Follow-Up task {} for document {}: {}", 
                                task.getTaskID(), documentNumber, e.getMessage());
                        taskCloseFailed = true;
                        createOscDocuments.add(CaseStatusResponse.CreateOscDocument.builder()
                                .documentNumber(documentNumber)
                                .reason("Failed to close BPM Follow-Up task: " + e.getMessage())
                                .oscType("Close BPM Follow-Up")
                                .build());
                    }
                }
            }

            // If we need to move to a queue and follow-up was open
            if (queueName != null && !queueName.isEmpty() && isFollowupOpen && !taskCloseFailed) {
                try {
                    onBaseService.manageOnbaseCase(onbaseCaseId, clientCode, queueName);
                    log.info("Successfully moved case {} to queue {} for document {}", onbaseCaseId, queueName, documentNumber);
                    successfulDocuments.add(documentNumber);
                } catch (Exception e) {
                    log.error("Failed to move case to queue {} for document {}: {}", queueName, documentNumber, e.getMessage());
                    String oscType = queueName.contains("Returning") ? "Move to CP Returning" : "Move to DV Post Complete";
                    createOscDocuments.add(CaseStatusResponse.CreateOscDocument.builder()
                            .documentNumber(documentNumber)
                            .reason("Failed to move case to queue: " + e.getMessage())
                            .oscType(oscType)
                            .build());
                }
            } else if ((queueName == null || queueName.isEmpty()) && !taskCloseFailed) {
                // Just closing BPM follow-up (no queue move)
                if (isFollowupOpen) {
                    successfulDocuments.add(documentNumber);
                } else {
                    log.info("No open BPM Follow-Up tasks found for document: {}", documentNumber);
                    failedDocuments.add(CaseStatusResponse.FailedDocument.builder()
                            .documentNumber(documentNumber)
                            .reason("No open BPM Follow-Up tasks found")
                            .build());
                }
            } else if (!isFollowupOpen && queueName != null && !queueName.isEmpty()) {
                // No follow-up was open but we still need to move the case
                try {
                    onBaseService.manageOnbaseCase(onbaseCaseId, clientCode, queueName);
                    log.info("Successfully moved case {} to queue {} for document {} (no follow-up was open)", 
                            onbaseCaseId, queueName, documentNumber);
                    successfulDocuments.add(documentNumber);
                } catch (Exception e) {
                    log.error("Failed to move case to queue {} for document {}: {}", queueName, documentNumber, e.getMessage());
                    String oscType = queueName.contains("Returning") ? "Move to CP Returning" : "Move to DV Post Complete";
                    createOscDocuments.add(CaseStatusResponse.CreateOscDocument.builder()
                            .documentNumber(documentNumber)
                            .reason("Failed to move case to queue: " + e.getMessage())
                            .oscType(oscType)
                            .build());
                }
            }

        } catch (Exception e) {
            log.error("Error processing document {}: {}", documentNumber, e.getMessage(), e);
            String oscType = queueName == null ? "Close BPM Follow-Up" : 
                    (queueName.contains("Returning") ? "Move to CP Returning" : "Move to DV Post Complete");
            createOscDocuments.add(CaseStatusResponse.CreateOscDocument.builder()
                    .documentNumber(documentNumber)
                    .reason("Processing error: " + e.getMessage())
                    .oscType(oscType)
                    .build());
        }
    }

    /**
     * Build the response object
     */
    private CaseStatusResponse buildResponse(int totalSubmitted, 
                                              List<String> successfulDocuments,
                                              List<CaseStatusResponse.FailedDocument> failedDocuments,
                                              List<CaseStatusResponse.CreateOscDocument> createOscDocuments,
                                              String message) {
        return CaseStatusResponse.builder()
                .totalSubmitted(totalSubmitted)
                .successCount(successfulDocuments.size())
                .failedCount(failedDocuments.size())
                .createOscCount(createOscDocuments.size())
                .successfulDocuments(successfulDocuments)
                .failedDocuments(failedDocuments)
                .createOscDocuments(createOscDocuments)
                .message(message)
                .build();
    }

    /**
     * Extract document numbers that have a "Data entry" task from the given list
     * 
     * @param documentNumbers List of document numbers to check
     * @return List of document numbers that have a "Data entry" task
     */
    public List<String> getDocumentsWithDataEntryTask(List<String> documentNumbers) {
        log.info("Extracting documents with 'Data entry' task from {} document numbers", documentNumbers.size());
        
        try {
            // Query MongoDB for documents with Data entry task
            List<CaseInstanceDocument> caseInstances = caseInstanceRepository
                    .findByDocumentNumbersAndTaskName(documentNumbers, "Data entry");
            
            // Extract document numbers from identifiers
            List<String> documentsWithDataEntry = caseInstances.stream()
                    .flatMap(caseInstance -> caseInstance.getIdentifiers().stream())
                    .filter(identifier -> documentNumbers.contains(identifier.getValue()))
                    .map(CaseInstanceDocument.Identifier::getValue)
                    .distinct()
                    .collect(Collectors.toList());
            
            log.info("Found {} documents with 'Data entry' task out of {} submitted", 
                    documentsWithDataEntry.size(), documentNumbers.size());
            
            return documentsWithDataEntry;
            
        } catch (Exception e) {
            log.error("Error extracting documents with Data entry task: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to extract documents with Data entry task: " + e.getMessage());
        }
    }

    /**
     * Extract document numbers that do NOT have a "Data entry" task from the given list
     * 
     * @param documentNumbers List of document numbers to check
     * @return List of document numbers that do NOT have a "Data entry" task
     */
    public List<String> getDocumentsWithoutDataEntryTask(List<String> documentNumbers) {
        log.info("Extracting documents without 'Data entry' task from {} document numbers", documentNumbers.size());
        
        List<String> documentsWithDataEntry = getDocumentsWithDataEntryTask(documentNumbers);
        
        List<String> documentsWithoutDataEntry = documentNumbers.stream()
                .filter(doc -> !documentsWithDataEntry.contains(doc))
                .collect(Collectors.toList());
        
        log.info("Found {} documents without 'Data entry' task", documentsWithoutDataEntry.size());
        
        return documentsWithoutDataEntry;
    }
}
