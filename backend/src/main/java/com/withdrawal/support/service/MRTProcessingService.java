package com.withdrawal.support.service;

import com.withdrawal.support.dto.CaseDetails;
import com.withdrawal.support.dto.DataEntryCase;
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

    public void processMrtWaitingCases() {
        try{

            //call out manual review
            processWaitingCase("Withdrawal_GIACT_Validation_MRT","Event_mrt_response_received","Call Out Manual Review");

            //external approval task
            processWaitingCase("Withdrawal_GIACT_Validation_MRT","Event_approval","External PI Exception Approval");

            //approval case
            processWaitingCase("Withdrawal_Approval","Event_approval","PI Management Approval");

            //mrt case
            processWaitingCase("Withdrawal_MRT", "Event_mrt_response_received","Call Out Manual Review");
            // send email for all cases with event present
        } catch (Exception e) {

        }
    }

    boolean isAllCalloutTaskComplete(List<OnBaseCaseDetails.Task> taskList, String taskName) {
        if (taskList == null || taskList.isEmpty()) {
            log.debug("No tasks found");
            return false;
        }

        List<OnBaseCaseDetails.Task> calloutTaskList = taskList.stream()
                .filter(task -> taskName.equalsIgnoreCase(task.getTaskType()))
                .toList();

        if (calloutTaskList.isEmpty()) {
            log.debug("No call out tasks found");
            return false;
        }

        boolean allComplete = calloutTaskList.stream()
                .allMatch(task -> "Complete".equalsIgnoreCase(task.getStatus()));

        return allComplete;
    }

    private void processWaitingCase(String processDefinitionKey, String activityId, String taskName) {
        List<DataEntryCase> mrtWaitingCaseList = dataEntryService.getyWaitingCases(processDefinitionKey, activityId);

        List<String> caseWithEvent = new ArrayList<>();

        for (DataEntryCase mrtWaitingCase : mrtWaitingCaseList) {
            CaseDetails caseDetails = dataEntryService.getCaseDetails(mrtWaitingCase.getProcessInstanceId());
            OnBaseCaseDetails onBaseDetails = onBaseService.getOnBaseCaseDetails(
                    caseDetails.getClientCode(),
                    caseDetails.getCaseId()
            );

            List<OnBaseCaseDetails.Task> taskList = onBaseDetails.getTasks();
            boolean isAllTaskComplete = isAllCalloutTaskComplete(taskList, taskName);

            if (isAllTaskComplete) {
                caseWithEvent.add(onBaseDetails.getDocumentNumber());
            }
        }

        for (String doc : caseWithEvent) {
            System.out.println(doc);
        }
    }
}
