package com.withdrawal.support.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnBaseCaseDetails {
    private Integer statusCode;
    private String message;
    private Long caseID;
    private String createdBy;
    private String createdDate;
    private String documentNumber;
    private String status;
    private String isInvalid;
    private String queueName;
    private String contractNum;
    private Integer noteCount;
    private Integer taskCount;
    private Integer nigoCount;
    private Integer attachmentCount;
    private List<Object> attachments;
    private List<Note> notes;
    private List<Nigo> nigOs;
    private List<Task> tasks;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Note {
        private String submissionDate;
        private String commentCategory;
        private String commentSubCategory;
        private String commentDetail;
        private String comment;
        private String description;
        private String createdDate;
        private String createBy;
        private Long noteId;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Nigo {
        private String createdDate;
        private String createdBy;
        private String category;
        private String reason;
        private String detailedReason;
        private String status;
        private String resolutionDate;
        private String resolution;
        private Long nigoId;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Task {
        private String createdDate;
        private String createdBy;
        private Long taskID;
        private String taskType;
        private String status;
        private List<Object> attachments;
        private List<Object> notes;
        private List<Long> nigoIds;
    }
}





