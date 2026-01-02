package com.withdrawal.support.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "case_instance")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseInstanceDocument {
    
    @Id
    private String id;
    
    private List<Identifier> identifiers;
    private String status;
    private String correlationId;
    private String caseStatus;  // Additional status field from MongoDB
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Map<String, Object> metadata;
    private List<CaseTask> tasks;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Identifier {
        private String type;
        private String value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CaseTask {
        @Id
        private String id;

        private String status;
        private String taskType;
        private String taskName;
    }
}

