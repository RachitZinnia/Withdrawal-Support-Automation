package com.withdrawal.support.controller;

import com.withdrawal.support.dto.LetterGenerationResult;
import com.withdrawal.support.service.LetterProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/letter")
@RequiredArgsConstructor
@Slf4j
public class LetterProcessingController {

    private final LetterProcessingService letterProcessingService;

    /**
     * Process letter waiting cases and return summary result as JSON
     */
    @PostMapping("/process")
    public ResponseEntity<LetterGenerationResult> processLetterCases() {
        log.info("Received request to process letter waiting cases");
        
        try {
            LetterGenerationResult result = letterProcessingService.processLetterWaitingCases();
            log.info("Successfully processed {} letter cases", result.getTotalProcessed());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error processing letter cases", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(LetterGenerationResult.builder()
                            .message("Error processing letter cases: " + e.getMessage())
                            .processedAt(LocalDateTime.now())
                            .build());
        }
    }

    /**
     * Process letter waiting cases and download as Excel file
     */
    @GetMapping("/process/excel")
    public ResponseEntity<byte[]> processLetterCasesAndDownloadExcel() {
        log.info("Received request to process letter waiting cases and generate Excel");
        
        try {
            LetterGenerationResult result = letterProcessingService.processLetterWaitingCases();
            log.info("Successfully processed {} letter cases", result.getTotalProcessed());

            byte[] excelBytes = letterProcessingService.generateExcel(result);

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "letter_generation_data_" + timestamp + ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            headers.setContentLength(excelBytes.length);

            log.info("Generated Excel file: {} ({} bytes)", filename, excelBytes.length);
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error processing letter cases and generating Excel", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Health check endpoint for letter processing
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Letter Processing Service is running");
    }
}
