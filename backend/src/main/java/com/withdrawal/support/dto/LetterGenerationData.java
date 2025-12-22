package com.withdrawal.support.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing letter generation process data fetched from Camunda
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LetterGenerationData {
    
    // Business identifiers
    private String correspondenceCorrelationId;  // Used as business key for letter_generation_process
    private String documentNumber;               // From docAndCorrelationIdMapping
    
    // Camunda variables
    private String carrier;
    private String contractNumber;
    private String deliveryType;
    private String xmlFileName;
}
