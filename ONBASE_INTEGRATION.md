# OnBase Integration - Case Categorization

This document explains how the system integrates with OnBase Integration Manager and categorizes cases based on status and BPM Follow-Up tasks.

## 📡 OnBase API Endpoint

**Base URL:** `https://onbaseintegrationmanager.se2.com/api/OnBase`

**Endpoint:** `GET /GetCaseDetails`

**Query Parameters:**
- `request.lob` - Client code (LOB - Line of Business)
- `request.caseId` - OnBase case ID

**Full URL Example:**
```
https://onbaseintegrationmanager.se2.com/api/OnBase/GetCaseDetails?request.lob=CLIENT123&request.caseId=12533840
```

**Headers:**
```
Content-Type: application/json
Authorization: Basic dGhha3VyMzpXZWxjb21lQDEyMzQ=
```

## 📊 Response Format

```json
{
    "statusCode": 200,
    "message": "Success.",
    "caseID": 12533840,
    "createdBy": "OBSERVQA",
    "createdDate": "11/10/2025 04:18:41 AM",
    "documentNumber": "20251110-MAN-306993",
    "status": "New",
    "queueName": "TQA - New",
    "contractNum": "7280001036",
    "noteCount": 3,
    "taskCount": 3,
    "nigoCount": 1,
    "tasks": [
        {
            "createdDate": "11/10/2025 04:19:20 AM",
            "taskID": 12533842,
            "taskType": "BPM Follow-Up",
            "status": "Complete"
        },
        {
            "createdDate": "11/10/2025 04:25:16 AM",
            "taskID": 12533860,
            "taskType": "BPM Follow-Up",
            "status": "Pending"
        }
    ]
}
```

## 🎯 Case Categorization Logic

The system categorizes cases into three main categories based on the OnBase status and BPM Follow-Up task completion:

### Category 1: FOLLOW_UP_COMPLETE ✅
**Condition:** All BPM Follow-Up tasks have status = "Complete"

**Action:** These cases will be **cancelled** later

**Example:**
```json
{
    "status": "Any Status",
    "tasks": [
        { "taskType": "BPM Follow-Up", "status": "Complete" },
        { "taskType": "BPM Follow-Up", "status": "Complete" },
        { "taskType": "Other Task", "status": "Any" }
    ]
}
```

### Category 2: DV_POST_OPEN_DV_COMPLETE 🔄
**Condition:** 
- Status = "Post Complete" 
- AND at least one BPM Follow-Up task status ≠ "Complete"

**Action:** These cases will be **cancelled** later

**Example:**
```json
{
    "status": "Post Complete",
    "tasks": [
        { "taskType": "BPM Follow-Up", "status": "Complete" },
        { "taskType": "BPM Follow-Up", "status": "Pending" }
    ]
}
```

### Category 3: CHECK_MONGODB 🔍
**Condition:** 
- Status = "Pend" OR "Pending" OR "New"
- AND at least one BPM Follow-Up task status ≠ "Complete"

**Action:** Check MongoDB status to determine next steps

**Example:**
```json
{
    "status": "New",
    "tasks": [
        { "taskType": "BPM Follow-Up", "status": "Pending" }
    ]
}
```

**MongoDB Check:**
- If NOT in progress → Mark as PENDING in MongoDB
- If already in progress → Continue monitoring

## 🔄 Processing Flow

```
1. Fetch waiting cases from Camunda
   ↓
2. For each case, get clientCode and onbaseCaseId from Camunda
   ↓
3. Call OnBase API:
   GET /GetCaseDetails?request.lob={clientCode}&request.caseId={onbaseCaseId}
   ↓
4. Parse response to get:
   - status (e.g., "New", "Post Complete", "Pend")
   - tasks (array of all tasks)
   ↓
5. Filter tasks to find BPM Follow-Up tasks
   ↓
6. Check if ALL BPM Follow-Up tasks are Complete:
   
   YES → Category: FOLLOW_UP_COMPLETE
   ↓
   NO → Check status:
        
        If "Post Complete" → Category: DV_POST_OPEN_DV_COMPLETE
        
        If "Pend"/"Pending"/"New" → Category: CHECK_MONGODB
                                     ↓
                                     Query MongoDB for case status
                                     ↓
                                     If NOT in progress:
                                        - Mark as PENDING
                                        - Update MongoDB
                                     ↓
                                     If in progress:
                                        - Continue monitoring
```

## 🏗️ Backend Implementation

### DTOs

**OnBaseCaseDetails.java**
```java
@Data
public class OnBaseCaseDetails {
    private Long caseID;
    private String status;
    private Integer taskCount;
    private List<Task> tasks;
    
    @Data
    public static class Task {
        private Long taskID;
        private String taskType;
        private String status;
        private String createdDate;
    }
}
```

**CaseCategory.java**
```java
public enum CaseCategory {
    FOLLOW_UP_COMPLETE,           // All BPM Follow-Up tasks complete
    DV_POST_OPEN_DV_COMPLETE,    // Post Complete with incomplete tasks
    CHECK_MONGODB,                // Pend/Pending/New with incomplete tasks
    UNKNOWN                       // Other scenarios
}
```

### Service Methods

**OnBaseService.getOnBaseCaseDetails()**
```java
public OnBaseCaseDetails getOnBaseCaseDetails(String clientCode, String onbaseCaseId) {
    WebClient webClient = webClientBuilder
            .baseUrl(apiConfig.getOnbase().getUrl())
            .defaultHeader("Content-Type", "application/json")
            .defaultHeader("Authorization", apiConfig.getOnbase().getAuthorization())
            .build();

    return webClient.get()
            .uri(uriBuilder -> uriBuilder
                    .path("/GetCaseDetails")
                    .queryParam("request.lob", clientCode)
                    .queryParam("request.caseId", onbaseCaseId)
                    .build())
            .retrieve()
            .bodyToMono(OnBaseCaseDetails.class)
            .block();
}
```

**OnBaseService.categorizeCaseByStatus()**
```java
public CaseCategory categorizeCaseByStatus(OnBaseCaseDetails caseDetails) {
    boolean allBpmComplete = areAllBpmFollowUpTasksComplete(caseDetails.getTasks());
    
    // Category 1
    if (allBpmComplete) {
        return CaseCategory.FOLLOW_UP_COMPLETE;
    }
    
    // Category 2
    if ("Post Complete".equalsIgnoreCase(caseDetails.getStatus())) {
        return CaseCategory.DV_POST_OPEN_DV_COMPLETE;
    }
    
    // Category 3
    if (isStatusPendingOrNew(caseDetails.getStatus())) {
        return CaseCategory.CHECK_MONGODB;
    }
    
    return CaseCategory.UNKNOWN;
}

private boolean areAllBpmFollowUpTasksComplete(List<Task> tasks) {
    return tasks.stream()
            .filter(task -> "BPM Follow-Up".equalsIgnoreCase(task.getTaskType()))
            .allMatch(task -> "Complete".equalsIgnoreCase(task.getStatus()));
}
```

## 📝 Configuration

**application.properties**
```properties
# OnBase API Configuration
api.onbase.url=https://onbaseintegrationmanager.se2.com/api/OnBase
api.onbase.authorization=Basic dGhha3VyMzpXZWxjb21lQDEyMzQ=
```

## 🧪 Testing

### Test OnBase API Directly

```bash
curl --location 'https://onbaseintegrationmanager.se2.com/api/OnBase/GetCaseDetails?request.lob=CLIENT123&request.caseId=12533840' \
--header 'Content-Type: application/json' \
--header 'Authorization: Basic dGhha3VyMzpXZWxjb21lQDEyMzQ='
```

### Expected Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `status` | String | Case status (New, Pend, Pending, Post Complete, etc.) |
| `tasks` | Array | List of all tasks for the case |
| `tasks[].taskType` | String | Type of task (e.g., "BPM Follow-Up") |
| `tasks[].status` | String | Task status (Complete, Pending, etc.) |

## 📊 Categorization Examples

### Example 1: All BPM Follow-Up Complete
```json
{
    "status": "Processing",
    "tasks": [
        { "taskType": "BPM Follow-Up", "status": "Complete" },
        { "taskType": "BPM Follow-Up", "status": "Complete" },
        { "taskType": "Manual Review", "status": "Pending" }
    ]
}
```
**Category:** FOLLOW_UP_COMPLETE → Will cancel

### Example 2: Post Complete with Incomplete Tasks
```json
{
    "status": "Post Complete",
    "tasks": [
        { "taskType": "BPM Follow-Up", "status": "Complete" },
        { "taskType": "BPM Follow-Up", "status": "Pending" }
    ]
}
```
**Category:** DV_POST_OPEN_DV_COMPLETE → Will cancel

### Example 3: New with Incomplete Tasks
```json
{
    "status": "New",
    "tasks": [
        { "taskType": "BPM Follow-Up", "status": "Pending" }
    ]
}
```
**Category:** CHECK_MONGODB → Check MongoDB

## 🎯 Business Logic Summary

| OnBase Status | All BPM Complete? | Category | Action |
|--------------|-------------------|----------|--------|
| Any | ✅ Yes | FOLLOW_UP_COMPLETE | Cancel case |
| Post Complete | ❌ No | DV_POST_OPEN_DV_COMPLETE | Cancel case |
| Pend/Pending/New | ❌ No | CHECK_MONGODB | Check MongoDB, then decide |
| Other | ❌ No | UNKNOWN | Manual review |

## 🔍 Troubleshooting

### No Tasks Returned
- Verify case ID is correct
- Check if case exists in OnBase
- Verify API authorization

### All Cases Show UNKNOWN
- Check if tasks array is populated
- Verify task type spelling ("BPM Follow-Up")
- Check status values are correct

### MongoDB Check Failing
- Verify MongoDB connection
- Check case ID format
- Verify collection exists

## 📚 Related Documentation

- `CAMUNDA_INTEGRATION.md` - Camunda API integration
- `README.md` - System overview
- `CHANGES_CAMUNDA_VARIABLES.md` - Variable extraction details

---

**Last Updated:** November 10, 2025
**Version:** 1.2.0 (Added OnBase categorization)





