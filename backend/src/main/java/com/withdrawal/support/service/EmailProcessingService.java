package com.withdrawal.support.service;

import com.withdrawal.support.dto.DataEntryCase;
import com.withdrawal.support.dto.ProcessingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailProcessingService {

    private final DataEntryService dataEntryService;

    public ProcessingResult processEmailWaitingCases() {
        log.info("Starting email waiting cases processing");
        
        List<DataEntryCase> emailWaitingCaseList = dataEntryService.getyWaitingCases(
                "email_resolution_process", "Activity_14ejxxr");

        log.info("Found {} email waiting cases to process", emailWaitingCaseList.size());

        ProcessingResult result = ProcessingResult.builder()
                .documentNumbersToCancel(new ArrayList<>())
                .documentNumbersToComplete(new ArrayList<>())
                .documentNumbersForManualReview(new ArrayList<>())
                .documentNumbersToReturning(new ArrayList<>())
                .details(new ArrayList<>())
                .build();

        result.setTotalCases(emailWaitingCaseList.size());
        int successCount = 0;
        int failedCount = 0;

        for (DataEntryCase emailWaitingCase : emailWaitingCaseList) {
            try {
                String documentNumber = dataEntryService.getCamundaVariable(
                        emailWaitingCase.getProcessInstanceId(), "documentNumber");
                String emailCategory = dataEntryService.getEmaiInfoFromBusinessKey(documentNumber);
                
                log.info("Processing document: {} with category: {}", documentNumber, emailCategory);

                if ("COMPLETE".equalsIgnoreCase(emailCategory)) {
                    if (!result.getDocumentNumbersToComplete().contains(documentNumber)) {
                        result.getDocumentNumbersToComplete().add(documentNumber);
                    }
                    if (!result.getDocumentNumbersToCancel().contains(documentNumber)) {
                        result.getDocumentNumbersToCancel().add(documentNumber);
                    }
                    successCount++;
                } else if ("CANCEL".equalsIgnoreCase(emailCategory)) {
                    if (!result.getDocumentNumbersToCancel().contains(documentNumber)) {
                        result.getDocumentNumbersToCancel().add(documentNumber);
                    }
                    successCount++;
                } else {
                    if (!result.getDocumentNumbersForManualReview().contains(documentNumber)) {
                        result.getDocumentNumbersForManualReview().add(documentNumber);
                    }
                    result.setManualReviewRequired(result.getManualReviewRequired() + 1);
                }
            } catch (Exception e) {
                log.error("Error processing email case {}: {}", 
                        emailWaitingCase.getProcessInstanceId(), e.getMessage());
                failedCount++;
            }
        }

        result.setSuccessfulCases(successCount);
        result.setFailedCases(failedCount);
        
        result.setMessage(String.format(
                "Email Processing completed: %d total cases. To DV POST COMPLETE: %d, To Cancel: %d, Manual Review: %d",
                result.getTotalCases(),
                result.getDocumentNumbersToComplete().size(),
                result.getDocumentNumbersToCancel().size(),
                result.getDocumentNumbersForManualReview().size()
        ));

        log.info("Email processing completed: {}", result.getMessage());
        return result;
    }
}
