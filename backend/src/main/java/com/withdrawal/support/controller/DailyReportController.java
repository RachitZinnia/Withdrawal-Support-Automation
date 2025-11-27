package com.withdrawal.support.controller;

import com.withdrawal.support.dto.DailyReportProcessingResult;
import com.withdrawal.support.service.DailyReportProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/daily-report")
@RequiredArgsConstructor
@Slf4j
public class DailyReportController {

    private final DailyReportProcessingService dailyReportProcessingService;

    /**
     * Endpoint to upload and process daily report CSV file
     */
    @PostMapping("/upload")
    public ResponseEntity<DailyReportProcessingResult> uploadDailyReport(
            @RequestParam("file") MultipartFile file) {
        
        log.info("Received daily report upload request - File: {}, Size: {} bytes", 
                file.getOriginalFilename(), file.getSize());
        
        // Validate file
        if (file.isEmpty()) {
            log.error("Uploaded file is empty");
            return ResponseEntity.badRequest().body(
                    DailyReportProcessingResult.builder()
                            .message("Error: Uploaded file is empty")
                            .build()
            );
        }
        
        // Validate file type (CSV)
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            log.error("Invalid file type: {}", filename);
            return ResponseEntity.badRequest().body(
                    DailyReportProcessingResult.builder()
                            .message("Error: Only CSV files are accepted")
                            .build()
            );
        }
        
        try {
            DailyReportProcessingResult result = dailyReportProcessingService.processDailyReport(file);
            log.info("Daily report processing completed successfully");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error processing daily report", e);
            
            DailyReportProcessingResult errorResult = DailyReportProcessingResult.builder()
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
        return ResponseEntity.ok("Daily Report Service is running");
    }
}




