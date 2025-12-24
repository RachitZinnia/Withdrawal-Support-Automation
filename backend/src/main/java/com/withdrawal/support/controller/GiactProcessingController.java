package com.withdrawal.support.controller;

import com.withdrawal.support.dto.MRTProcessingResult;
import com.withdrawal.support.service.MRTProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mrt")
@RequiredArgsConstructor
@Slf4j
public class GiactProcessingController {

    private final MRTProcessingService mrtProcessingService;

    /**
     * Endpoint to process all MRT waiting cases across different scenarios
     * Returns cases with complete tasks and event received
     */
    @PostMapping("/process")
    public ResponseEntity<MRTProcessingResult> processMRTCases() {
        log.info("Received request to process MRT waiting cases");
        
        try {
            MRTProcessingResult result = mrtProcessingService.processMrtWaitingCases();
            log.info("MRT processing completed successfully");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error processing MRT cases", e);
            
            MRTProcessingResult errorResult = MRTProcessingResult.builder()
                    .totalCasesProcessed(0)
                    .casesWithCompleteTasksAndEvent(0)
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
        return ResponseEntity.ok("MRT Processing Service is running");
    }
}
