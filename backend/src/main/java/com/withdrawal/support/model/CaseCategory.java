package com.withdrawal.support.model;

public enum CaseCategory {
    FOLLOW_UP_COMPLETE,           // All BPM Follow-Up tasks complete (will cancel)
    DV_POST_OPEN_DV_COMPLETE,    // Status "Post Complete" but BPM Follow-Up not complete (will cancel)
    CHECK_MONGODB,                // Status Pend/Pending/New with BPM Follow-Up not complete
    WAITING_CASE,
    CASE_RETURNING,  //no active process instance and status in pend/new and bpm follow up open
    UNKNOWN// Other scenarios
}





