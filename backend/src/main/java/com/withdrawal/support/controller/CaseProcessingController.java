package com.withdrawal.support.controller;

import com.withdrawal.support.dto.ProcessingResult;
import com.withdrawal.support.service.CaseProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cases")
@RequiredArgsConstructor
@Slf4j
public class CaseProcessingController {

    private final CaseProcessingService caseProcessingService;

    /**
     * Endpoint to process all data entry waiting cases
     * This is the main endpoint that will be called from the UI
     */
    @PostMapping("/process-dataentry-waiting")
    public ResponseEntity<ProcessingResult> processDataEntryWaitingCases() {
        log.info("Received request to process data entry waiting cases");
        
        try {
            ProcessingResult result = caseProcessingService.processDataEntryWaitingCases();
            log.info("Processing completed successfully");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error processing data entry waiting cases", e);
            
            ProcessingResult errorResult = ProcessingResult.builder()
                    .totalCases(0)
                    .successfulCases(0)
                    .failedCases(0)
                    .manualReviewRequired(0)
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
        return ResponseEntity.ok("Service is running");
    }
}





