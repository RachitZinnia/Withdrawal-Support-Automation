package com.withdrawal.support.model;

public enum CaseCategory {
    FOLLOW_UP_COMPLETE,           // All BPM Follow-Up tasks complete (will cancel)
    DV_POST_OPEN_DV_COMPLETE,    // Status "Post Complete" but BPM Follow-Up not complete (will cancel)
    CHECK_MONGODB,                // Status Pend/Pending/New with BPM Follow-Up not complete
    UNKNOWN                       // Other scenarios
}





