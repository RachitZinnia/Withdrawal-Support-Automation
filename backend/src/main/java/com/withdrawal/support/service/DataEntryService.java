package com.withdrawal.support.service;

import com.withdrawal.support.config.ApiConfig;
import com.withdrawal.support.dto.*;
import com.withdrawal.support.model.ProcessResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataEntryService {
    
    /**
     * Result object containing process instance IDs and their state information
     */
    @Data
    @AllArgsConstructor
    public static class ProcessInstanceResult {
        private List<String> processInstanceIds;
        private boolean hasActiveInstances;
    }

    private final ApiConfig apiConfig;
    private final WebClient.Builder webClientBuilder;

    @Autowired
    OnBaseService onBaseService;

    /**
     * Fetches all data entry waiting cases from the Camunda BPM API
     * Endpoint: /execution?processDefinitionKey=dataentry&activityId=Event_0a7e4e6&active=true
     */
    public List<DataEntryCase> getDataEntryWaitingCases() {
        log.info("Fetching data entry waiting cases from Camunda BPM");

        try {
            WebClient webClient = webClientBuilder
                    .baseUrl(apiConfig.getDataentry().getUrl())
                    .build();

            List<DataEntryCase> cases = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/execution")
                            .queryParam("processDefinitionKey", "dataentry")
                            .queryParam("activityId", "Event_0a7e4e6")
                            .queryParam("active", "true")
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<DataEntryCase>>() {})
                    .onErrorResume(e -> {
                        log.error("Error fetching waiting cases from Camunda", e);
                        return Mono.just(List.of());
                    })
                    .block();

            log.info("Retrieved {} waiting cases from Camunda", cases != null ? cases.size() : 0);
            return cases != null ? cases : List.of();

        } catch (Exception e) {
            log.error("Failed to fetch data entry waiting cases from Camunda", e);
            throw new RuntimeException("Failed to fetch data entry waiting cases: " + e.getMessage());
        }
    }

    public List<DataEntryCase> getyWaitingCases(String processDefinitionKey, String activityId) {
        log.info("Fetching waiting cases from Camunda BPM");

        try {
            WebClient webClient = webClientBuilder
                    .baseUrl(apiConfig.getDataentry().getUrl())
                    .build();

            List<DataEntryCase> cases = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/execution")
                            .queryParam("processDefinitionKey", processDefinitionKey)
                            .queryParam("activityId", activityId)
                            .queryParam("active", "true")
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<DataEntryCase>>() {})
                    .onErrorResume(e -> {
                        log.error("Error fetching waiting cases from Camunda", e);
                        return Mono.just(List.of());
                    })
                    .block();

            log.info("Retrieved {} waiting cases from Camunda", cases != null ? cases.size() : 0);
            return cases != null ? cases : List.of();

        } catch (Exception e) {
            log.error("Failed to fetch data entry waiting cases from Camunda", e);
            throw new RuntimeException("Failed to fetch data entry waiting cases: " + e.getMessage());
        }
    }

    /**
     * Fetches case details from Camunda by getting process variables
     * Gets clientCode and onbaseCaseId from the process instance
     */
    public CaseDetails getCaseDetails(String processInstanceId) {
        log.info("Fetching case details for process instance: {}", processInstanceId);

        try {
            // Get clientCode variable
            String clientCode = getCamundaVariable(processInstanceId, "clientCode");
            log.info("Retrieved clientCode: {}", clientCode);

            // Get onbaseCaseId variable
            String onbaseCaseId = getCamundaVariable(processInstanceId, "onbaseCaseId");
            log.info("Retrieved onbaseCaseId: {}", onbaseCaseId);

            // Build client variables map
            Map<String, Object> clientVariables = new HashMap<>();
            clientVariables.put("clientCode", clientCode);
            clientVariables.put("processInstanceId", processInstanceId);

            // Build and return case details
            CaseDetails details = CaseDetails.builder()
                    .caseId(onbaseCaseId)
                    .caseReference(processInstanceId)
                    .clientCode(clientCode)
                    .clientVariables(clientVariables)
                    .build();

            log.info("Successfully retrieved case details - OnBase Case ID: {}, Client Code: {}",
                    onbaseCaseId, clientCode);
            return details;

        } catch (Exception e) {
            log.error("Failed to fetch case details for process instance: {}", processInstanceId, e);
            throw new RuntimeException("Failed to fetch case details: " + e.getMessage());
        }
    }

    /**
     * Gets a specific variable from a Camunda process instance using history API
     * Endpoint: /history/variable-instance?processInstanceId={processInstanceId}&variableName={variableName}
     */
    public String  getCamundaVariable(String processInstanceId, String variableName) {
        log.debug("Fetching Camunda variable '{}' for process instance: {}", variableName, processInstanceId);

        try {
            WebClient webClient = webClientBuilder
                    .baseUrl(apiConfig.getDataentry().getUrl())
                    .build();

            List<CamundaVariable> variables = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/history/variable-instance")
                            .queryParam("processInstanceId", processInstanceId)
                            .queryParam("variableName", variableName)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<CamundaVariable>>() {})
                    .onErrorResume(e -> {
                        log.warn("Error fetching variable '{}' for process instance {}: {}",
                                variableName, processInstanceId, e.getMessage());
                        return Mono.just(List.of());
                    })
                    .block();

            if (variables != null && !variables.isEmpty() && variables.get(0).getValue() != null) {
                String value = variables.get(0).getValue();
                log.debug("Variable '{}' value: {}", variableName, value);
                return value;
            } else {
                log.warn("Variable '{}' not found or has no value for process instance: {}",
                        variableName, processInstanceId);
                return null;
            }

        } catch (Exception e) {
            log.error("Failed to fetch Camunda variable '{}' for process instance: {}",
                    variableName, processInstanceId, e);
            return null;
        }
    }

    /**
     * Gets process instance IDs from a business key with processDefinitionKey filtering
     * Priority logic:
     * 1. First, look for processDefinitionKey = "ocr_processing" - if found, return ONLY that process instance ID
     * 2. If not found, look for processDefinitionKey = "dataentry" - return ALL matching process instance IDs
     *
     * Endpoint: /history/process-instance?processInstanceBusinessKey={businessKey}
     */
    public ProcessInstanceResult getProcessInstanceIdsByBusinessKey(String businessKey, String definitonKey) {
        log.info("Fetching process instance IDs for business key: {}", businessKey);

        try {
            WebClient webClient = webClientBuilder
                    .baseUrl(apiConfig.getDataentry().getUrl())
                    .build();

            List<Map<String, Object>> processInstances = new ArrayList<>();
            if (definitonKey.isEmpty()) {
                processInstances = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/history/process-instance")
                                .queryParam("processInstanceBusinessKey", businessKey)
                                .build())
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                        .onErrorResume(e -> {
                            log.error("Error fetching process instances for business key {}: {}",
                                    businessKey, e.getMessage());
                            return Mono.just(List.of());
                        })
                        .block();
            } else {
                processInstances = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/history/process-instance")
                                .queryParam("processInstanceBusinessKey", businessKey).queryParam("processDefinitionKey", definitonKey)
                                .build())
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                        .onErrorResume(e -> {
                            log.error("Error fetching process instances for business key {}: {}",
                                    businessKey, e.getMessage());
                            return Mono.just(List.of());
                        })
                        .block();
            }

            if (processInstances == null || processInstances.isEmpty()) {
                log.warn("No process instances found for business key: {}", businessKey);
                return new ProcessInstanceResult(List.of(), false);
            }

            log.info("Found {} total process instances for business key: {}",
                    processInstances.size(), businessKey);
            
            // Check if any instances are ACTIVE
            boolean hasActiveInstances = processInstances.stream()
                    .anyMatch(instance -> {
                        String instanceState = (String) instance.get("state");
                        boolean isActive = instanceState != null && instanceState.equalsIgnoreCase("ACTIVE");
                        if (isActive) {
                            log.debug("Found ACTIVE instance: {}", instance.get("id"));
                        }
                        return isActive;
                    });
            
            if (hasActiveInstances) {
                log.info("Found ACTIVE process instance(s) for business key: {}", businessKey);
            }
            if (!definitonKey.isEmpty()) {
                List<String> result = processInstances.stream().map(instance -> (String)instance.get("id")).collect(Collectors.toList());
                return new ProcessInstanceResult(result, hasActiveInstances);
            }
            // Debug: Log first instance structure to verify field names
            if (!processInstances.isEmpty()) {
                log.debug("First instance keys: {}", processInstances.get(0).keySet());
                log.debug("First instance processDefinitionKey: {}", processInstances.get(0).get("processDefinitionKey"));
                log.debug("First instance processDefinitionId: {}", processInstances.get(0).get("processDefinitionId"));
            }
            
            // Priority 1: Check for ocr_processing process instances
            List<String> ocrProcessingIds = processInstances.stream()
                    .filter(instance -> {
                        String processDefKey = getProcessDefinitionKey(instance);
                        boolean isOcrProcessing = "ocr_processing".equals(processDefKey);
                        if (isOcrProcessing) {
                            log.debug("Found ocr_processing instance: {}", instance.get("id"));
                        }
                        return isOcrProcessing;
                    })
                    .map(instance -> (String) instance.get("id"))
                    .filter(id -> id != null && !id.isEmpty())
                    .toList();

            if (!ocrProcessingIds.isEmpty()) {
                log.info("Found {} ocr_processing process instance(s) for business key: {} - Returning ocr_processing ID only",
                        ocrProcessingIds.size(), businessKey);
                // Return only the first ocr_processing instance ID
                return new ProcessInstanceResult(List.of(ocrProcessingIds.get(0)), hasActiveInstances);
            }

            // Priority 2: If no ocr_processing found, look for dataentry process instances
            List<String> dataEntryIds = processInstances.stream()
                    .filter(instance -> {
                        String processDefKey = getProcessDefinitionKey(instance);
                        boolean isDataEntry = "dataentry".equals(processDefKey);
                        if (isDataEntry) {
                            log.debug("Found dataentry instance: {}", instance.get("id"));
                        }
                        return isDataEntry;
                    })
                    .map(instance -> (String) instance.get("id"))
                    .filter(id -> id != null && !id.isEmpty())
                    .toList();

            if (!dataEntryIds.isEmpty()) {
                log.info("No ocr_processing found. Found {} dataentry process instance(s) for business key: {} - Returning all dataentry IDs",
                        dataEntryIds.size(), businessKey);
                return new ProcessInstanceResult(dataEntryIds, hasActiveInstances);
            }

            // Priority 3: If neither ocr_processing nor dataentry found, return empty
            log.warn("No ocr_processing or dataentry process instances found for business key: {}", businessKey);
            log.debug("Available process definitions: {}",
                    processInstances.stream()
                            .map(this::getProcessDefinitionKey)
                            .distinct()
                            .toList());
            
            return new ProcessInstanceResult(List.of(), hasActiveInstances);

        } catch (Exception e) {
            log.error("Failed to fetch process instances for business key: {}", businessKey, e);
            return new ProcessInstanceResult(List.of(), false);
        }
    }


    public String  getEmaiInfoFromBusinessKey(String businessKey) {

        try {
            WebClient webClient = webClientBuilder
                    .baseUrl(apiConfig.getDataentry().getUrl())
                    .build();

            List<Map<String, Object>> processInstances = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/history/process-instance")
                            .queryParam("processInstanceBusinessKey", businessKey)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .onErrorResume(e -> {
                        log.error("Error fetching process instances for business key {}: {}",
                                businessKey, e.getMessage());
                        return Mono.just(List.of());
                    })
                    .block();

            List<String> letterIds = processInstances.stream()
                    .filter(instance -> {
                        String processDefKey = getProcessDefinitionKey(instance);
                        boolean isLetterProcess = "letter_resolution_process".equals(processDefKey);
                        boolean isActive = "ACTIVE".equalsIgnoreCase((String) instance.get("state"));
                        if (isLetterProcess && isActive) {
                            log.debug("Found Active letter_resolution_process instance: {}", instance.get("id"));
                        }
                        return isLetterProcess && isActive;
                    }).map(instance -> (String) instance.get("id"))
                    .filter(id -> id != null && !id.isEmpty())
                    .toList();

            TreeMap<String, String> startTimeAndDefKeyMap = new TreeMap<>(Comparator.reverseOrder());

            if (letterIds.isEmpty()) {
                processInstances.stream().forEach(instance ->{
                    startTimeAndDefKeyMap.put((String)instance.get("startTime"), (String)instance.get("processDefinitionKey"));
                });

                String firstValue = "";
                if (!startTimeAndDefKeyMap.isEmpty()) {
                    firstValue = startTimeAndDefKeyMap.firstEntry().getValue();
                }

                if (firstValue.equalsIgnoreCase("letter_resolution_process")) {
                    return "COMPLETE";
                } else {
                    String withdrawalId = processInstances.stream()
                            .filter(instance -> {
                                String processDefKey = getProcessDefinitionKey(instance);
                                boolean iswithdrawalProcess = "withdrawal".equals(processDefKey);
                                if (iswithdrawalProcess) {
                                    log.debug("Found withdrawal instance: {}", instance.get("id"));
                                }
                                return iswithdrawalProcess;
                            })
                            .map(instance -> (String) instance.get("id"))
                            .filter(id -> id != null && !id.isEmpty())
                            .toList().get(0);

                    String onbaseCaseId = getCamundaVariable(withdrawalId, "onbaseCaseId");
                    String clientCode = getCamundaVariable(withdrawalId, "clientCode");
                    OnBaseCaseDetails caseDetails = onBaseService.getOnBaseCaseDetails(clientCode, onbaseCaseId);
                    List<OnBaseCaseDetails.Task> bpmFollowUpTasks = caseDetails.getTasks().stream()
                            .filter(task -> "BPM Follow-Up".equalsIgnoreCase(task.getTaskType()))
                            .toList();

                    int total = bpmFollowUpTasks.size();
                    int closed = (int) bpmFollowUpTasks.stream()
                            .filter(task -> "Complete".equalsIgnoreCase(task.getStatus()))
                            .count();
                    int open = total - closed;
                    if (open == 0) {
                        return "CANCEL";
                    }
                }
            }

        } catch (Exception e) {
            return "MANUAL_REVIEW";
        }
        return "MANUAL_REVIEW";
    }
    
    /**
     * Extracts processDefinitionKey from process instance map
     * The processDefinitionKey can be in either "processDefinitionKey" field or extracted from "processDefinitionId"
     */
    private String getProcessDefinitionKey(Map<String, Object> instance) {
        // Try direct processDefinitionKey field first
        Object processDefKey = instance.get("processDefinitionKey");
        if (processDefKey != null) {
            String key = processDefKey.toString();
            log.debug("Found processDefinitionKey directly: {}", key);
            return key;
        }
        
        // Try to extract from processDefinitionId (format: "processKey:version:deploymentId")
        Object processDefinitionId = instance.get("processDefinitionId");
        if (processDefinitionId != null) {
            String defId = processDefinitionId.toString();
            log.debug("Extracting from processDefinitionId: {}", defId);
            int colonIndex = defId.indexOf(':');
            if (colonIndex > 0) {
                String extractedKey = defId.substring(0, colonIndex);
                log.debug("Extracted processDefinitionKey: {}", extractedKey);
                return extractedKey;
            }
            return defId;
        }
        
        // Fallback: try definitionId field as well
        Object definitionId = instance.get("definitionId");
        if (definitionId != null) {
            String defId = definitionId.toString();
            log.debug("Extracting from definitionId: {}", defId);
            int colonIndex = defId.indexOf(':');
            if (colonIndex > 0) {
                return defId.substring(0, colonIndex);
            }
            return defId;
        }
        
        log.warn("Could not find processDefinitionKey in instance. Available keys: {}", instance.keySet());
        return null;
    }

    public ProcessResult getStartDateFromProcessInstance(String processInstanceId) {
        WebClient webClient = webClientBuilder
                .baseUrl(apiConfig.getDataentry().getUrl())
                .build();

        List<ProcessDetail> processDetails = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/history/process-instance")
                        .queryParam("processInstanceId", processInstanceId)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<ProcessDetail>>() {})
                .onErrorResume(e -> {
                    log.warn("Error for process instance {}: {}", processInstanceId, e.getMessage());
                    return Mono.just(List.of());
                })
                .block();
        if (Objects.nonNull(processDetails)) {
            ProcessDetail processDetail = processDetails.get(0);
            if (Objects.nonNull(processDetail)) {
                DateTimeFormatter formatter =
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

                OffsetDateTime offsetDateTime =
                        OffsetDateTime.parse(processDetail.getStartTime(), formatter);
                return new ProcessResult(processDetail.getBusinessKey(), offsetDateTime.toLocalDate());
            }

        }
        return new ProcessResult();
    }
}

