package com.withdrawal.support.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnBaseManageCaseRequest {
    private String lob;
    private String caseID;
    private String queueName;
}



