package com.withdrawal.support.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MRTProcessingResult {
    private int totalCasesProcessed;
    private int casesWithCompleteTasksAndEvent;
    
    // Combined list of all document numbers with complete tasks and event received
    @Builder.Default
    private List<String> casesWithCompleteTasksAndEventList = new ArrayList<>();
    
    private String message;
}

