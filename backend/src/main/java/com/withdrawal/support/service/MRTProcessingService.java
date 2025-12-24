package com.withdrawal.support.service;

import com.withdrawal.support.dto.CaseDetails;
import com.withdrawal.support.dto.DataEntryCase;
import com.withdrawal.support.dto.MRTProcessingResult;
import com.withdrawal.support.dto.OnBaseCaseDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MRTProcessingService {

    @Autowired
    DataEntryService dataEntryService;

    @Autowired
    OnBaseService onBaseService;

    public MRTProcessingResult processMrtWaitingCases() {
        log.info("Starting MRT waiting cases processing");
        
        MRTProcessingResult result = MRTProcessingResult.builder()
                .casesWithCompleteTasksAndEventList(new ArrayList<>())
                .build();

        int totalCasesProcessed = 0;

        try {
            // Scenario 1: Call Out Manual Review (GIACT Validation)
            log.info("Processing scenario: Call Out Manual Review (GIACT Validation)");
            List<String> callOutDocs = processWaitingCase(
                    "Withdrawal_GIACT_Validation_MRT",
                    "Event_mrt_response_received",
                    "Call Out Manual Review (GIACT)"
            );
            addUniqueDocuments(result.getCasesWithCompleteTasksAndEventList(), callOutDocs);
            totalCasesProcessed += callOutDocs.size();

            // Scenario 2: External PI Exception Approval
            log.info("Processing scenario: External PI Exception Approval");
            List<String> externalApprovalDocs = processWaitingCase(
                    "Withdrawal_GIACT_Validation_MRT",
                    "Event_approval",
                    "External PI Exception Approval"
            );
            addUniqueDocuments(result.getCasesWithCompleteTasksAndEventList(), externalApprovalDocs);
            totalCasesProcessed += externalApprovalDocs.size();

            // Scenario 3: PI Management Approval
            log.info("Processing scenario: PI Management Approval");
            List<String> piApprovalDocs = processWaitingCase(
                    "Withdrawal_Approval",
                    "Event_approval",
                    "PI Management Approval"
            );
            addUniqueDocuments(result.getCasesWithCompleteTasksAndEventList(), piApprovalDocs);
            totalCasesProcessed += piApprovalDocs.size();

            // Scenario 4: MRT Call Out Manual Review
            log.info("Processing scenario: MRT Call Out Manual Review");
            List<String> mrtDocs = processWaitingCase(
                    "Withdrawal_MRT",
                    "Event_mrt_response_received",
                    "MRT Call Out Manual Review"
            );
            addUniqueDocuments(result.getCasesWithCompleteTasksAndEventList(), mrtDocs);
            totalCasesProcessed += mrtDocs.size();

            result.setTotalCasesProcessed(totalCasesProcessed);
            result.setCasesWithCompleteTasksAndEvent(result.getCasesWithCompleteTasksAndEventList().size());
            
            result.setMessage(String.format(
                    "MRT Processing completed: %d total cases processed, %d unique cases with complete tasks and event received",
                    totalCasesProcessed,
                    result.getCasesWithCompleteTasksAndEventList().size()
            ));
            
            log.info("MRT processing completed: {}", result.getMessage());

        } catch (Exception e) {
            log.error("Error during MRT processing", e);
            result.setMessage("Processing failed: " + e.getMessage());
        }

        return result;
    }
    
    /**
     * Adds documents to the target list only if they don't already exist (avoid duplicates)
     */
    private void addUniqueDocuments(List<String> targetList, List<String> newDocs) {
        for (String doc : newDocs) {
            if (doc != null && !targetList.contains(doc)) {
                targetList.add(doc);
            }
        }
    }

    /**
     * Checks if all tasks EXCEPT "BPM Follow-Up" are complete
     * Returns true only if there are non-BPM Follow-Up tasks and all of them are complete
     */
    boolean areAllNonBpmFollowUpTasksComplete(List<OnBaseCaseDetails.Task> taskList) {
        if (taskList == null || taskList.isEmpty()) {
            log.debug("No tasks found");
            return false;
        }

        // Filter out BPM Follow-Up tasks - we want all OTHER tasks
        List<OnBaseCaseDetails.Task> nonBpmFollowUpTasks = taskList.stream()
                .filter(task -> !"BPM Follow-Up".equalsIgnoreCase(task.getTaskType()))
                .toList();

        if (nonBpmFollowUpTasks.isEmpty()) {
            log.debug("No non-BPM Follow-Up tasks found");
            return false;
        }

        boolean allComplete = nonBpmFollowUpTasks.stream()
                .allMatch(task -> "Complete".equalsIgnoreCase(task.getStatus()));

        log.debug("Found {} non-BPM Follow-Up tasks, all complete: {}", nonBpmFollowUpTasks.size(), allComplete);
        
        if (allComplete) {
            log.info("All {} non-BPM Follow-Up tasks are complete", nonBpmFollowUpTasks.size());
        }
        
        return allComplete;
    }

    private List<String> processWaitingCase(String processDefinitionKey, String activityId, String scenarioName) {
        List<String> caseWithEvent = new ArrayList<>();
        
        try {
            List<DataEntryCase> mrtWaitingCaseList = dataEntryService.getyWaitingCases(processDefinitionKey, activityId);
            log.info("Found {} waiting cases for {} / {} ({})", 
                    mrtWaitingCaseList.size(), processDefinitionKey, activityId, scenarioName);

            for (DataEntryCase mrtWaitingCase : mrtWaitingCaseList) {
                try {
                    CaseDetails caseDetails = dataEntryService.getCaseDetails(mrtWaitingCase.getProcessInstanceId());
                    OnBaseCaseDetails onBaseDetails = onBaseService.getOnBaseCaseDetails(
                            caseDetails.getClientCode(),
                            caseDetails.getCaseId()
                    );

                    List<OnBaseCaseDetails.Task> taskList = onBaseDetails.getTasks();
                    
                    // Check if all tasks EXCEPT BPM Follow-Up are complete
                    boolean allNonBpmTasksComplete = areAllNonBpmFollowUpTasksComplete(taskList);

                    if (allNonBpmTasksComplete) {
                        String docNumber = onBaseDetails.getDocumentNumber();
                        if (docNumber != null && !caseWithEvent.contains(docNumber)) {
                            caseWithEvent.add(docNumber);
                            log.info("Added document {} - all non-BPM Follow-Up tasks complete ({})", 
                                    docNumber, scenarioName);
                        }
                    }
                } catch (Exception e) {
                    log.error("Error processing case {}: {}", mrtWaitingCase.getProcessInstanceId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error fetching waiting cases for {} / {}: {}", processDefinitionKey, activityId, e.getMessage());
        }

        log.info("Scenario '{}' completed with {} documents with event present", scenarioName, caseWithEvent.size());
        return caseWithEvent;
    }
}
