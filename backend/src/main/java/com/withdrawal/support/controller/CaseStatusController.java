package com.withdrawal.support.controller;

import com.withdrawal.support.dto.CaseStatusRequest;
import com.withdrawal.support.dto.CaseStatusResponse;
import com.withdrawal.support.service.CaseStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/case/status")
@RequiredArgsConstructor
@Slf4j
public class CaseStatusController {

    private final CaseStatusService caseStatusService;

    /**
     * Close BPM Follow-Up tasks for the given document numbers
     */
    @PostMapping("/close/followup")
    public ResponseEntity<CaseStatusResponse> closeBpmFollowUpTask(@RequestBody CaseStatusRequest request) {
        log.info("Received request to close BPM Follow-Up for {} documents", 
                request.getDocumentNumbers() != null ? request.getDocumentNumbers().size() : 0);
        
        try {
            if (request.getDocumentNumbers() == null || request.getDocumentNumbers().isEmpty()) {
                return ResponseEntity.badRequest().body(CaseStatusResponse.builder()
                        .message("No document numbers provided")
                        .totalSubmitted(0)
                        .successCount(0)
                        .failedCount(0)
                        .createOscCount(0)
                        .build());
            }
            
            CaseStatusResponse response = caseStatusService.closeBpmnFollowUpTask(request.getDocumentNumbers());
            log.info("Close BPM Follow-Up completed - Success: {}, Failed: {}, Create OSC: {}", 
                    response.getSuccessCount(), response.getFailedCount(), response.getCreateOscCount());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error processing close BPM Follow-Up request: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(CaseStatusResponse.builder()
                    .message("Error processing request: " + e.getMessage())
                    .totalSubmitted(request.getDocumentNumbers() != null ? request.getDocumentNumbers().size() : 0)
                    .successCount(0)
                    .failedCount(0)
                    .createOscCount(0)
                    .build());
        }
    }

    /**
     * Move cases to CP Returning queue
     */
    @PostMapping("/move/returning")
    public ResponseEntity<CaseStatusResponse> moveCaseToCpReturning(@RequestBody CaseStatusRequest request) {
        log.info("Received request to move {} documents to CP Returning", 
                request.getDocumentNumbers() != null ? request.getDocumentNumbers().size() : 0);
        
        try {
            if (request.getDocumentNumbers() == null || request.getDocumentNumbers().isEmpty()) {
                return ResponseEntity.badRequest().body(CaseStatusResponse.builder()
                        .message("No document numbers provided")
                        .totalSubmitted(0)
                        .successCount(0)
                        .failedCount(0)
                        .createOscCount(0)
                        .build());
            }
            
            CaseStatusResponse response = caseStatusService.moveCaseToCpReturning(request.getDocumentNumbers());
            log.info("Move to CP Returning completed - Success: {}, Failed: {}, Create OSC: {}", 
                    response.getSuccessCount(), response.getFailedCount(), response.getCreateOscCount());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error processing move to CP Returning request: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(CaseStatusResponse.builder()
                    .message("Error processing request: " + e.getMessage())
                    .totalSubmitted(request.getDocumentNumbers() != null ? request.getDocumentNumbers().size() : 0)
                    .successCount(0)
                    .failedCount(0)
                    .createOscCount(0)
                    .build());
        }
    }

    /**
     * Move cases to DV Post Complete queue
     */
    @PostMapping("/move/complete")
    public ResponseEntity<CaseStatusResponse> moveCaseToDvPostComplete(@RequestBody CaseStatusRequest request) {
        log.info("Received request to move {} documents to DV Post Complete", 
                request.getDocumentNumbers() != null ? request.getDocumentNumbers().size() : 0);
        
        try {
            if (request.getDocumentNumbers() == null || request.getDocumentNumbers().isEmpty()) {
                return ResponseEntity.badRequest().body(CaseStatusResponse.builder()
                        .message("No document numbers provided")
                        .totalSubmitted(0)
                        .successCount(0)
                        .failedCount(0)
                        .createOscCount(0)
                        .build());
            }
            
            CaseStatusResponse response = caseStatusService.moveCaseToDvPostComplete(request.getDocumentNumbers());
            log.info("Move to DV Post Complete completed - Success: {}, Failed: {}, Create OSC: {}", 
                    response.getSuccessCount(), response.getFailedCount(), response.getCreateOscCount());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error processing move to DV Post Complete request: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(CaseStatusResponse.builder()
                    .message("Error processing request: " + e.getMessage())
                    .totalSubmitted(request.getDocumentNumbers() != null ? request.getDocumentNumbers().size() : 0)
                    .successCount(0)
                    .failedCount(0)
                    .createOscCount(0)
                    .build());
        }
    }
}
