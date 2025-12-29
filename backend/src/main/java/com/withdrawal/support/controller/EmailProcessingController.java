package com.withdrawal.support.controller;

import com.withdrawal.support.dto.ProcessingResult;
import com.withdrawal.support.service.EmailProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
@Slf4j
public class EmailProcessingController {

    private final EmailProcessingService emailProcessingService;

    /**
     * Process email waiting cases and return categorized results
     */
    @PostMapping("/process")
    public ResponseEntity<ProcessingResult> processEmailCases() {
        log.info("Received request to process email waiting cases");
        
        try {
            ProcessingResult result = emailProcessingService.processEmailWaitingCases();
            log.info("Email processing completed successfully");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error processing email cases", e);
            
            ProcessingResult errorResult = ProcessingResult.builder()
                    .totalCases(0)
                    .successfulCases(0)
                    .failedCases(0)
                    .message("Error: " + e.getMessage())
                    .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Email Processing Service is running");
    }
}
