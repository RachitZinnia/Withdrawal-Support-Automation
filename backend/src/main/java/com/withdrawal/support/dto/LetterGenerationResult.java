package com.withdrawal.support.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO representing the result of letter generation processing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LetterGenerationResult {
    
    // Summary statistics
    private int totalProcessed;
    
    // Variable statistics
    private int carrierFound;
    private int contractNumberFound;
    private int deliveryTypeFound;
    private int xmlFileNameFound;
    
    // Processing metadata
    private LocalDateTime processedAt;
    private String excelFileName;
    
    // Detailed data
    private List<LetterGenerationData> data;
    
    // Message
    private String message;
}
