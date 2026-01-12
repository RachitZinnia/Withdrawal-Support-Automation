package com.withdrawal.support.service;

import com.withdrawal.support.config.ApiConfig;
import com.withdrawal.support.dto.OnBaseCaseDetails;
import com.withdrawal.support.dto.OnBaseManageCaseRequest;
import com.withdrawal.support.dto.OnBaseManageTaskRequest;
import com.withdrawal.support.dto.OnBaseManageTaskResponse;
import com.withdrawal.support.model.CaseCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnBaseService {

    private final ApiConfig apiConfig;
    private final WebClient.Builder webClientBuilder;

    /**
     * Fetches case details from OnBase Integration Manager
     * Endpoint: /GetCaseDetails?request.lob={clientCode}&request.caseId={caseId}
     */
    public OnBaseCaseDetails getOnBaseCaseDetails(String clientCode, String onbaseCaseId) {
        log.info("Fetching OnBase case details for clientCode: {}, caseId: {}", clientCode, onbaseCaseId);
        
        try {
            WebClient webClient = webClientBuilder
                    .baseUrl(apiConfig.getOnbase().getUrl())
                    .defaultHeader("Content-Type", "application/json")
                    .defaultHeader("Authorization", apiConfig.getOnbase().getAuthorization())
                    .build();

            OnBaseCaseDetails details = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/GetCaseDetails")
                            .queryParam("request.lob", clientCode)
                            .queryParam("request.caseId", onbaseCaseId)
                            .build())
                    .retrieve()
                    .bodyToMono(OnBaseCaseDetails.class)
                    .block();

            if (details != null) {
                log.info("Retrieved OnBase details - Status: {}, TaskCount: {}, DocumentNumber: {}", 
                        details.getStatus(), details.getTaskCount(), details.getDocumentNumber());
            }
            
            return details;
            
        } catch (Exception e) {
            log.error("Failed to fetch OnBase case details for clientCode: {}, caseId: {}", 
                    clientCode, onbaseCaseId, e);
            throw new RuntimeException("Failed to fetch OnBase case details: " + e.getMessage());
        }
    }
    
    /**
     * Categorizes a case based on its status and BPM Follow-Up task completion
     * 
     * Categories:
     * - FOLLOW_UP_COMPLETE: All BPM Follow-Up tasks are complete (will cancel)
     * - DV_POST_OPEN_DV_COMPLETE: Status "Post Complete" but BPM Follow-Up not complete (will cancel)
     * - CHECK_MONGODB: Status Pend/Pending/New with BPM Follow-Up not complete
     */
    public CaseCategory categorizeCaseByStatus(OnBaseCaseDetails caseDetails) {
        if (caseDetails == null || caseDetails.getTasks() == null) {
            log.warn("Case details or tasks are null, returning UNKNOWN category");
            return CaseCategory.UNKNOWN;
        }
        
        String status = caseDetails.getStatus();
        boolean allBpmFollowUpComplete = areAllBpmFollowUpTasksComplete(caseDetails.getTasks());
        boolean anyBpmFollowUpNotComplete = !allBpmFollowUpComplete;
        
        log.info("Case categorization - Status: {}, All BPM Follow-Up Complete: {}", 
                status, allBpmFollowUpComplete);
        
        // Category 1: All BPM Follow-Up tasks are complete
        if (allBpmFollowUpComplete) {
            log.info("Category: FOLLOW_UP_COMPLETE - All BPM Follow-Up tasks complete");
            return CaseCategory.FOLLOW_UP_COMPLETE;
        }
        
        // Category 2: Status "Post Complete" but BPM Follow-Up not complete
        if ("Post Complete".equalsIgnoreCase(status) && anyBpmFollowUpNotComplete) {
            log.info("Category: DV_POST_OPEN_DV_COMPLETE - Status 'Post Complete' but BPM Follow-Up not complete");
            return CaseCategory.DV_POST_OPEN_DV_COMPLETE;
        }
        
        // Category 3: Status Pend/Pending/New with BPM Follow-Up not complete
        if (isStatusPendingOrNew(status) && anyBpmFollowUpNotComplete) {
            log.info("Category: CHECK_MONGODB - Status '{}' with BPM Follow-Up not complete", status);
            return CaseCategory.CHECK_MONGODB;
        }
        
        log.info("Category: UNKNOWN - No specific category matched");
        return CaseCategory.UNKNOWN;
    }
    
    /**
     * Checks if all BPM Follow-Up tasks are complete
     */
    private boolean areAllBpmFollowUpTasksComplete(List<OnBaseCaseDetails.Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            log.debug("No tasks found");
            return false;
        }
        
        List<OnBaseCaseDetails.Task> bpmFollowUpTasks = tasks.stream()
                .filter(task -> "BPM Follow-Up".equalsIgnoreCase(task.getTaskType()))
                .toList();
        
        if (bpmFollowUpTasks.isEmpty()) {
            log.debug("No BPM Follow-Up tasks found");
            return false;
        }
        
        boolean allComplete = bpmFollowUpTasks.stream()
                .allMatch(task -> "Complete".equalsIgnoreCase(task.getStatus()));
        
        log.debug("Found {} BPM Follow-Up tasks, all complete: {}", bpmFollowUpTasks.size(), allComplete);
        
        return allComplete;
    }
    
    /**
     * Checks if status is Pend, Pending, or New
     */
    private boolean isStatusPendingOrNew(String status) {
        if (status == null) {
            return false;
        }
        
        return status.equalsIgnoreCase("Pend") ||
               status.equalsIgnoreCase("Pending") ||
               status.equalsIgnoreCase("New");
    }

    /**
     * Gets a human-readable description of the case category
     */
    public String getCategoryDescription(CaseCategory category) {
        return switch (category) {
            case FOLLOW_UP_COMPLETE -> "All BPM Follow-Up tasks complete - Will review manually";
            case DV_POST_OPEN_DV_COMPLETE -> "Status 'Post Complete' with incomplete BPM Follow-Up - Will cancel";
            case CHECK_MONGODB -> "Status Pend/Pending/New with incomplete BPM Follow-Up - Check MongoDB";
            case WAITING_CASE -> "active process instance present";
            case CASE_RETURNING -> "No active process instance, status Pend/New with BPM Follow-Up open - CP Returning";
            case UNKNOWN -> "Unknown category - Requires manual review";
        };
    }

    /**
     * Represents the BPM Follow-Up status summary
     */
    public static class BpmFollowUpStatus {
        private final int total;
        private final int open;
        private final int closed;
        
        public BpmFollowUpStatus(int total, int open, int closed) {
            this.total = total;
            this.open = open;
            this.closed = closed;
        }
        
        public int getTotal() { return total; }
        public int getOpen() { return open; }
        public int getClosed() { return closed; }
        
        public boolean isAllClosed() {
            return total > 0 && open == 0;
        }
        
        public String getStatusText() {
            if (total == 0) {
                return "N/A";
            } else if (open == 0) {
                return "All Closed";
            } else {
                return "Open (" + open + " of " + total + ")";
            }
        }
    }

    /**
     * Gets the BPM Follow-Up status for a case
     * Returns a summary of total, open, and closed BPM Follow-Up tasks
     */
    public BpmFollowUpStatus getBpmFollowUpStatus(OnBaseCaseDetails caseDetails) {
        if (caseDetails == null || caseDetails.getTasks() == null) {
            log.debug("Case details or tasks are null, returning N/A status");
            return new BpmFollowUpStatus(0, 0, 0);
        }
        
        List<OnBaseCaseDetails.Task> bpmFollowUpTasks = caseDetails.getTasks().stream()
                .filter(task -> "BPM Follow-Up".equalsIgnoreCase(task.getTaskType()))
                .toList();
        
        int total = bpmFollowUpTasks.size();
        int closed = (int) bpmFollowUpTasks.stream()
                .filter(task -> "Complete".equalsIgnoreCase(task.getStatus()))
                .count();
        int open = total - closed;
        
        log.debug("BPM Follow-Up status - Total: {}, Open: {}, Closed: {}", total, open, closed);
        
        return new BpmFollowUpStatus(total, open, closed);
    }

    /**
     * Manages OnBase task by moving it to a specified queue
     * Endpoint: GET /ManageTask with JSON body
     * 
     * @param taskId The task ID to manage
     * @param clientCode The LOB/client code (e.g., "USAA")
     * @param queueName The target queue name (e.g., "tp - exit {admin}")
     * @return OnBaseManageTaskResponse with statusCode and message
     */
    public OnBaseManageTaskResponse manageOnbaseTask(String taskId, String clientCode, String queueName) {
        log.info("Managing OnBase task - TaskID: {}, LOB: {}, QueueName: {}", taskId, clientCode, queueName);
        
        try {
            WebClient webClient = webClientBuilder
                    .baseUrl(apiConfig.getOnbase().getUrl())
                    .defaultHeader("Content-Type", "application/json")
                    .defaultHeader("Authorization", apiConfig.getOnbase().getAuthorization())
                    .build();

            OnBaseManageTaskRequest request = OnBaseManageTaskRequest.builder()
                    .lob(clientCode)
                    .taskID(taskId)
                    .queueName(queueName)
                    .build();

            OnBaseManageTaskResponse response = webClient
                    .method(HttpMethod.POST)
                    .uri("/ManageTask")
                    .body(BodyInserters.fromValue(request))
                    .retrieve()
                    .bodyToMono(OnBaseManageTaskResponse.class)
                    .block();

            if (response != null) {
                log.info("ManageTask response - StatusCode: {}, Message: {}", 
                        response.getStatusCode(), response.getMessage());
            }

            return response;

        } catch (Exception e) {
            log.error("Failed to manage OnBase task - TaskID: {}, LOB: {}, QueueName: {}", 
                    taskId, clientCode, queueName, e);
            throw new RuntimeException("Failed to manage OnBase task: " + e.getMessage());
        }
    }

    /**
     * Manages OnBase case by moving it to a specified queue
     * Endpoint: POST /ManageCase with JSON body
     * 
     * @param caseId The case ID to manage
     * @param clientCode The LOB/client code (e.g., "USAA")
     * @param queueName The target queue name (e.g., "CP - BPM Complete")
     * @return OnBaseManageTaskResponse with statusCode and message
     */
    public OnBaseManageTaskResponse manageOnbaseCase(String caseId, String clientCode, String queueName) {
        log.info("Managing OnBase case - CaseID: {}, LOB: {}, QueueName: {}", caseId, clientCode, queueName);
        
        try {
            WebClient webClient = webClientBuilder
                    .baseUrl(apiConfig.getOnbase().getUrl())
                    .defaultHeader("Content-Type", "application/json")
                    .defaultHeader("Authorization", apiConfig.getOnbase().getAuthorization())
                    .build();

            OnBaseManageCaseRequest request = OnBaseManageCaseRequest.builder()
                    .lob(clientCode)
                    .caseID(caseId)
                    .queueName(queueName)
                    .build();

            OnBaseManageTaskResponse response = webClient
                    .post()
                    .uri("/ManageCase")
                    .body(BodyInserters.fromValue(request))
                    .retrieve()
                    .bodyToMono(OnBaseManageTaskResponse.class)
                    .block();

            if (response != null) {
                log.info("ManageCase response - StatusCode: {}, Message: {}", 
                        response.getStatusCode(), response.getMessage());
            }

            return response;

        } catch (Exception e) {
            log.error("Failed to manage OnBase case - CaseID: {}, LOB: {}, QueueName: {}", 
                    caseId, clientCode, queueName, e);
            throw new RuntimeException("Failed to manage OnBase case: " + e.getMessage());
        }
    }
}

