package com.withdrawal.support.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnBaseManageTaskRequest {
    private String lob;
    private String taskID;
    private String queueName;
}



