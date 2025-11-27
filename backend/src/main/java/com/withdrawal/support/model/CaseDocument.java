package com.withdrawal.support.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "cases")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseDocument {
    
    @Id
    private String id;
    
    private String caseId;
    private String caseReference;
    private CaseStatus status;
    private LocalDateTime createdDate;
    private LocalDateTime lastUpdated;
    private Map<String, Object> metadata;
    private String notes;
}





