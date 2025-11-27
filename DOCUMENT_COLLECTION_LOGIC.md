# Document Number Collection Logic

This document explains how the system collects document numbers for cancellation and manual review.

## 🎯 Overview

The system categorizes cases and collects document numbers into two lists:
1. **Document Numbers to Cancel** - Cases that should be cancelled in Camunda Cockpit
2. **Document Numbers for Manual Review** - Cases that are IN_PROGRESS but stale (>2 business days)

## 📊 Collection Rules

### 1. Documents to Cancel (3 Scenarios)

#### Scenario A: All BPM Follow-Up Tasks Complete
**Category:** `FOLLOW_UP_COMPLETE`

**Condition:**
- All tasks with `taskType = "BPM Follow-Up"` have `status = "Complete"`

**Action:**
- Add `documentNumber` to cancellation list
- Mark case as COMPLETED

**Example:**
```json
{
    "documentNumber": "20251110-MAN-306993",
    "tasks": [
        { "taskType": "BPM Follow-Up", "status": "Complete" },
        { "taskType": "BPM Follow-Up", "status": "Complete" }
    ]
}
```
**Result:** `"20251110-MAN-306993"` → Cancel list

---

#### Scenario B: Post Complete with Incomplete BPM Follow-Up
**Category:** `DV_POST_OPEN_DV_COMPLETE`

**Condition:**
- OnBase status = `"Post Complete"`
- At least one "BPM Follow-Up" task is NOT "Complete"

**Action:**
- Add `documentNumber` to cancellation list
- Mark case as COMPLETED

**Example:**
```json
{
    "documentNumber": "20251109-EM-267711",
    "status": "Post Complete",
    "tasks": [
        { "taskType": "BPM Follow-Up", "status": "Complete" },
        { "taskType": "BPM Follow-Up", "status": "Pending" }
    ]
}
```
**Result:** `"20251109-EM-267711"` → Cancel list

---

#### Scenario C: MongoDB caseStatus NOT IN_PROGRESS
**Category:** `CHECK_MONGODB`

**Condition:**
- OnBase status = `"Pend"` OR `"Pending"` OR `"New"`
- At least one "BPM Follow-Up" task is NOT "Complete"
- MongoDB query result: `caseStatus ≠ "IN_PROGRESS"`

**Action:**
- Query MongoDB: `{ "identifiers.value": documentNumber }`
- Extract `caseStatus` field
- If `caseStatus` is NOT `"IN_PROGRESS"` → Add to cancellation list

**Example:**
```javascript
// MongoDB Document
{
    "identifiers": [
        { "value": "20251020-EM-267711" }
    ],
    "caseStatus": "PENDING"  // NOT IN_PROGRESS
}
```
**Result:** `"20251020-EM-267711"` → Cancel list

---

### 2. Documents for Manual Review

#### Scenario: IN_PROGRESS but Stale (>2 Business Days)
**Category:** `CHECK_MONGODB` (sub-case)

**Condition:**
- OnBase status = `"Pend"` OR `"Pending"` OR `"New"`
- At least one "BPM Follow-Up" task is NOT "Complete"
- MongoDB `caseStatus` = `"IN_PROGRESS"`
- `lastUpdated` date is more than 2 business days old

**Action:**
- Add `documentNumber` to manual review list
- Mark case as MANUAL_REVIEW_REQUIRED

**Example:**
```javascript
// MongoDB Document
{
    "identifiers": [
        { "value": "20251105-EM-100001" }
    ],
    "caseStatus": "IN_PROGRESS",
    "lastUpdated": "2025-11-07T10:00:00Z"  // 3+ days ago
}
```
**Result:** `"20251105-EM-100001"` → Manual Review list

## 🔄 Complete Flow Diagram

```
For each waiting case:
    ↓
Get OnBase Details (includes documentNumber)
    ↓
Categorize Case
    ↓
    ├─ FOLLOW_UP_COMPLETE
    │  └─ Add documentNumber → Cancel List
    │
    ├─ DV_POST_OPEN_DV_COMPLETE
    │  └─ Add documentNumber → Cancel List
    │
    ├─ CHECK_MONGODB
    │  ↓
    │  Query MongoDB for documentNumber
    │  ↓
    │  Extract caseStatus
    │  ↓
    │  ├─ caseStatus ≠ IN_PROGRESS
    │  │  └─ Add documentNumber → Cancel List
    │  │
    │  └─ caseStatus = IN_PROGRESS
    │     ↓
    │     Check lastUpdated date
    │     ↓
    │     ├─ > 2 business days
    │     │  └─ Add documentNumber → Manual Review List
    │     │
    │     └─ ≤ 2 business days
    │        └─ Continue monitoring (no action)
    │
    └─ UNKNOWN
       └─ Add documentNumber → Manual Review List
```

## 📝 Code Implementation

### ProcessingResult DTO

```java
public class ProcessingResult {
    // Summary counts
    private int totalCases;
    private int successfulCases;
    private int failedCases;
    private int manualReviewRequired;
    
    // Document number collections
    private List<String> documentNumbersToCancel;           // To cancel
    private List<String> documentNumbersForManualReview;    // Manual review
    
    private List<CaseProcessingDetail> details;  // All case details
}
```

### Collection Logic

**FOLLOW_UP_COMPLETE:**
```java
case FOLLOW_UP_COMPLETE -> {
    if (!result.getDocumentNumbersToCancel().contains(documentNumber)) {
        result.getDocumentNumbersToCancel().add(documentNumber);
    }
}
```

**DV_POST_OPEN_DV_COMPLETE:**
```java
case DV_POST_OPEN_DV_COMPLETE -> {
    if (!result.getDocumentNumbersToCancel().contains(documentNumber)) {
        result.getDocumentNumbersToCancel().add(documentNumber);
    }
}
```

**CHECK_MONGODB:**
```java
case CHECK_MONGODB -> {
    String caseStatus = caseMongoService.getCaseStatusFromMongo(documentNumber);
    boolean notInProgress = caseMongoService.isNotInProgress(documentNumber);
    
    if (notInProgress) {
        // NOT IN_PROGRESS → Cancel
        if (!result.getDocumentNumbersToCancel().contains(documentNumber)) {
            result.getDocumentNumbersToCancel().add(documentNumber);
        }
    } else {
        // IS IN_PROGRESS → Check if stale
        boolean isStale = caseMongoService.isInProgressAndStale(
            documentNumber, 
            businessConfig.getDaysThreshold()
        );
        
        if (isStale) {
            // Stale → Manual Review
            if (!result.getDocumentNumbersForManualReview().contains(documentNumber)) {
                result.getDocumentNumbersForManualReview().add(documentNumber);
            }
        }
    }
}
```

## 📊 MongoDB caseStatus Field

The system now uses the `caseStatus` field from MongoDB:

```javascript
{
    "identifiers": [{ "value": "20251020-EM-267711" }],
    "status": "active",           // General status
    "caseStatus": "IN_PROGRESS",  // ← This field is checked
    "lastUpdated": "2025-11-10T..."
}
```

**Recognized IN_PROGRESS Values:**
- `"IN_PROGRESS"`
- `"in progress"`
- `"in_progress"`
- `"inprogress"`
(case-insensitive)

## 📤 API Response Format

```json
{
    "totalCases": 100,
    "successfulCases": 70,
    "failedCases": 5,
    "manualReviewRequired": 10,
    
    "documentNumbersToCancel": [
        "20251110-MAN-306993",
        "20251109-EM-267711",
        "20251020-EM-267712"
    ],
    
    "documentNumbersForManualReview": [
        "20251105-EM-100001",
        "20251106-EM-100002"
    ],
    
    "message": "Processing completed: 100 total, 70 successful, 5 failed, 10 require manual review. Document numbers to cancel: 3, Manual review needed: 2",
    
    "details": [...]
}
```

## 🎯 Use Cases

### Use Case 1: All Scenarios Combined

**Input: 5 waiting cases**

1. Case A: All BPM Complete
   - `documentNumber: "DOC001"`
   - Result: → Cancel List

2. Case B: Post Complete + Incomplete BPM
   - `documentNumber: "DOC002"`
   - Result: → Cancel List

3. Case C: MongoDB caseStatus = "PENDING"
   - `documentNumber: "DOC003"`
   - Result: → Cancel List

4. Case D: MongoDB caseStatus = "IN_PROGRESS", recent
   - `documentNumber: "DOC004"`
   - Result: → Continue monitoring (no list)

5. Case E: MongoDB caseStatus = "IN_PROGRESS", 3 days old
   - `documentNumber: "DOC005"`
   - Result: → Manual Review List

**Final Result:**
```json
{
    "documentNumbersToCancel": ["DOC001", "DOC002", "DOC003"],
    "documentNumbersForManualReview": ["DOC005"]
}
```

## 🔧 Configuration

**Business Days Threshold:**
```properties
# application.properties
business.days-threshold=2
```

This threshold determines when an IN_PROGRESS case is considered "stale".

## 📋 Next Steps (Cancellation in Camunda)

Once you have the `documentNumbersToCancel` list:

1. **Manual Cancellation:**
   - Open Camunda Cockpit
   - Search for each document number
   - Cancel the process instance

2. **Automated Cancellation (Future):**
   - Create API endpoint to cancel multiple process instances
   - Call Camunda REST API:
     ```
     DELETE /process-instance/{id}
     ```

## 🔍 Logging

The system logs all document number collections:

```
[INFO] Added document 20251110-MAN-306993 to cancellation list (FOLLOW_UP_COMPLETE)
[INFO] Added document 20251109-EM-267711 to cancellation list (DV_POST_OPEN_DV_COMPLETE)
[INFO] Added document 20251020-EM-267712 to cancellation list (MongoDB NOT IN_PROGRESS)
[WARN] Added document 20251105-EM-100001 to manual review list (stale IN_PROGRESS)
[INFO] Document numbers to cancel: [20251110-MAN-306993, 20251109-EM-267711, 20251020-EM-267712]
[INFO] Document numbers for manual review: [20251105-EM-100001]
```

## ✅ Summary Table

| Scenario | Category | MongoDB caseStatus | Age | Action |
|----------|----------|-------------------|-----|---------|
| All BPM Complete | FOLLOW_UP_COMPLETE | N/A | N/A | → Cancel List |
| Post Complete + Incomplete BPM | DV_POST_OPEN_DV_COMPLETE | N/A | N/A | → Cancel List |
| Pend/Pending/New + NOT IN_PROGRESS | CHECK_MONGODB | NOT IN_PROGRESS | N/A | → Cancel List |
| Pend/Pending/New + IN_PROGRESS | CHECK_MONGODB | IN_PROGRESS | ≤ 2 days | → Continue monitoring |
| Pend/Pending/New + IN_PROGRESS | CHECK_MONGODB | IN_PROGRESS | > 2 days | → Manual Review List |
| Unknown | UNKNOWN | N/A | N/A | → Manual Review List |

---

**Last Updated:** November 10, 2025
**Version:** 1.5.0 (Added Document Collection Logic)





