package com.withdrawal.support.service;

import com.withdrawal.support.config.ApiConfig;
import com.withdrawal.support.dto.CamundaVariable;
import com.withdrawal.support.dto.CaseDetails;
import com.withdrawal.support.dto.DataEntryCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataEntryService {

    private final ApiConfig apiConfig;
    private final WebClient.Builder webClientBuilder;

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
     * Gets a specific variable from a Camunda process instance
     * Endpoint: /process-instance/{process_instance_id}/variables/{variable_name}
     */
    private String getCamundaVariable(String processInstanceId, String variableName) {
        log.debug("Fetching Camunda variable '{}' for process instance: {}", variableName, processInstanceId);
        
        try {
            WebClient webClient = webClientBuilder
                    .baseUrl(apiConfig.getDataentry().getUrl())
                    .build();

            CamundaVariable variable = webClient.get()
                    .uri("/process-instance/{processInstanceId}/variables/{variableName}", 
                            processInstanceId, variableName)
                    .retrieve()
                    .bodyToMono(CamundaVariable.class)
                    .onErrorResume(e -> {
                        log.warn("Error fetching variable '{}' for process instance {}: {}", 
                                variableName, processInstanceId, e.getMessage());
                        return Mono.empty();
                    })
                    .block();

            if (variable != null && variable.getValue() != null) {
                log.debug("Variable '{}' value: {}", variableName, variable.getValue());
                return variable.getValue();
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
     * Gets process instance IDs from a business key
     * Endpoint: /process-instance?businessKey={businessKey}
     */
    public List<String> getProcessInstanceIdsByBusinessKey(String businessKey) {
        log.info("Fetching process instance IDs for business key: {}", businessKey);
        
        try {
            WebClient webClient = webClientBuilder
                    .baseUrl(apiConfig.getDataentry().getUrl())
                    .build();

            List<Map<String, Object>> processInstances = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/process-instance")
                            .queryParam("businessKey", businessKey)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .onErrorResume(e -> {
                        log.error("Error fetching process instances for business key {}: {}", 
                                businessKey, e.getMessage());
                        return Mono.just(List.of());
                    })
                    .block();

            if (processInstances == null || processInstances.isEmpty()) {
                log.warn("No process instances found for business key: {}", businessKey);
                return List.of();
            }
            
            // Extract process instance IDs from response
            List<String> processInstanceIds = processInstances.stream()
                    .map(instance -> (String) instance.get("id"))
                    .filter(id -> id != null && !id.isEmpty())
                    .toList();
            
            log.info("Found {} process instances for business key: {}", processInstanceIds.size(), businessKey);
            return processInstanceIds;
            
        } catch (Exception e) {
            log.error("Failed to fetch process instances for business key: {}", businessKey, e);
            return List.of();
        }
    }
}

