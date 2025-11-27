# Daily Report Monitoring Feature

Complete documentation for the Daily Report CSV processing feature.

## 🎯 Overview

This feature automates the processing of daily monitoring reports that come in CSV format. The system:
1. Accepts CSV file upload via UI
2. Filters for "Not Matching" cases
3. Extracts business keys
4. Processes each business key through Camunda and OnBase
5. Categorizes and collects document numbers for action

## 📊 CSV File Format

### Required Columns

| Column Name | Description | Example |
|------------|-------------|---------|
| Camunda Business Key | Business key from Camunda | `BKEY123` |
| Camunda Client Code | Client code from Camunda | `CLIENT001` |
| Camunda Contract Number | Contract number | `7280001036` |
| Camunda Start Time | Process start time | `2025-11-10 10:30:00` |
| OnBase Business Key | Business key from OnBase | `BKEY123` |
| OnBase ClientCode | Client code from OnBase | `CLIENT001` |
| OnBase CaseQueue | Case queue name | `TQA - New` |
| OnBase ContractNumber | Contract number | `7280001036` |
| **Match** | **Matching status** | **"Not Matching"** ← Filter on this |
| Document Processing Date | Processing date | `11/10/2025` |
| Aging (In Days) | Days since creation | `3` |
| OnBase PendingCallOut | Pending callout flag | `Yes/No` |
| OnBase Notes | Additional notes | `Notes here` |

### CSV Example

```csv
Camunda Business Key,Camunda Client Code,Camunda Contract Number,Camunda Start Time,OnBase Business Key,OnBase ClientCode,OnBase CaseQueue,OnBase ContractNumber,Match,Document Processing Date,Aging (In Days),OnBase PendingCallOut,OnBase Notes
BKEY123,CLIENT001,7280001036,2025-11-10 10:30:00,BKEY123,CLIENT001,TQA - New,7280001036,Not Matching,11/10/2025,3,No,Case pending review
BKEY456,CLIENT002,7280001037,2025-11-09 14:20:00,,CLIENT002,TQA - Complete,7280001037,Matching,11/09/2025,2,No,Case completed
BKEY789,CLIENT003,7280001038,2025-11-08 09:15:00,BKEY789,CLIENT003,TQA - New,7280001038,Not Matching,11/08/2025,4,Yes,Requires follow-up
```

## 🔄 Processing Flow

```
1. User uploads CSV file in UI
   ↓
2. Backend: Parse CSV file
   ↓
3. Filter rows where Match = "Not Matching"
   ↓
4. Extract business keys:
   - Use OnBase Business Key if available
   - Otherwise use Camunda Business Key
   ↓
5. For each business key:
   
   5a. Call Camunda API:
       GET /process-instance?businessKey={businessKey}
       → Returns list of process instances
   
   5b. For each process instance ID:
       
       i.   Get clientCode variable
       ii.  Get onbaseCaseId variable
       iii. Call OnBase GetCaseDetails
       iv.  Categorize case
       v.   Collect document numbers
   ↓
6. Return aggregated results:
   - Document numbers to cancel
   - Document numbers to returning
   - Document numbers to complete
   - Document numbers for manual review
```

## 🏗️ Backend Architecture

### DTOs

**DailyReportRow.java**
```java
public class DailyReportRow {
    private String camundaBusinessKey;
    private String onBaseBusinessKey;
    private String match;  // "Matching" or "Not Matching"
    // ... all CSV columns
    
    public String getBusinessKeyToUse() {
        return onBaseBusinessKey != null && !onBaseBusinessKey.isEmpty() 
                ? onBaseBusinessKey 
                : camundaBusinessKey;
    }
    
    public boolean shouldProcess() {
        return "Not Matching".equalsIgnoreCase(match);
    }
}
```

**DailyReportProcessingResult.java**
```java
public class DailyReportProcessingResult {
    private int totalRowsInCsv;
    private int notMatchingRows;
    private int processedCases;
    
    private List<String> businessKeysExtracted;
    private List<String> documentNumbersToCancel;
    private List<String> documentNumbersToReturning;
    private List<String> documentNumbersToComplete;
    private List<String> documentNumbersForManualReview;
    
    private List<CaseProcessingDetail> details;
}
```

### Services

**CsvProcessingService.java**
- `parseCsvFile()` - Parses CSV into DailyReportRow objects
- `filterNotMatchingRows()` - Filters for "Not Matching"
- `extractBusinessKeys()` - Extracts unique business keys

**DailyReportProcessingService.java**
- `processDailyReport()` - Main orchestration method
- `processBusinessKey()` - Processes a single business key
- `processProcessInstanceFromBusinessKey()` - Processes instance similar to data entry

**DataEntryService.java** (Updated)
- `getProcessInstanceIdsByBusinessKey()` - NEW: Gets instances from business key

### Controller

**DailyReportController.java**
```java
@PostMapping("/upload")
public ResponseEntity<DailyReportProcessingResult> uploadDailyReport(
        @RequestParam("file") MultipartFile file) {
    // Validates CSV file
    // Calls processing service
    // Returns results
}
```

## 🌐 API Endpoints

### Upload Daily Report

**Endpoint:** `POST /api/daily-report/upload`

**Content-Type:** `multipart/form-data`

**Parameters:**
- `file` - CSV file (multipart file upload)

**Response:**
```json
{
    "totalRowsInCsv": 150,
    "notMatchingRows": 25,
    "processedCases": 30,
    "successfulCases": 20,
    "failedCases": 2,
    "manualReviewRequired": 8,
    
    "businessKeysExtracted": ["BKEY123", "BKEY456", "BKEY789"],
    
    "documentNumbersToCancel": [
        "20251110-MAN-306993",
        "20251109-EM-267711"
    ],
    
    "documentNumbersToReturning": [
        "20251108-EM-267712"
    ],
    
    "documentNumbersToComplete": [
        "20251107-EM-267713"
    ],
    
    "documentNumbersForManualReview": [
        "20251105-EM-100001"
    ],
    
    "details": [ /* Case-by-case details */ ]
}
```

## 🎨 Frontend Features

### New Tab: "Daily Report Monitoring"

**Components:**
- Tab navigation (Data Entry / Daily Report)
- CSV file upload area with drag-and-drop styling
- File validation (CSV only, max 10MB)
- Upload & Process button
- Results display with statistics
- Four separate document number lists

### UI Flow

```
1. User clicks "Daily Report Monitoring" tab
   ↓
2. Click upload area to select CSV file
   ↓
3. File name and size displayed
   ↓
4. Click "Upload & Process" button
   ↓
5. Loading state with spinner
   ↓
6. Results displayed:
   - Statistics cards
   - Document number lists (4 categories)
   - Detailed case table
```

## 📋 Document Number Collections

### 1. To Cancel
**Sources:**
- All BPM Follow-Up tasks complete
- Status "Post Complete" with incomplete BPM
- MongoDB caseStatus NOT IN_PROGRESS

**Color:** Red

### 2. To Returning
**Sources:**
- MongoDB caseStatus NOT IN_PROGRESS (from CHECK_MONGODB)

**Color:** Blue

### 3. To Complete
**Sources:**
- DV_POST_OPEN_DV_COMPLETE category

**Color:** Green

### 4. Manual Review
**Sources:**
- IN_PROGRESS but stale (>2 business days)
- Unknown category

**Color:** Orange

## 🔌 Camunda API Integration

### Get Process Instances by Business Key

**Endpoint:** `GET /process-instance?businessKey={businessKey}`

**Example Request:**
```
GET https://bpm.se2.com/engine-rest/process-instance?businessKey=BKEY123
```

**Example Response:**
```json
[
    {
        "id": "88205faa-bc6f-11f0-a49b-02c29394f0e3",
        "definitionId": "dataentry:1:...",
        "businessKey": "BKEY123",
        "caseInstanceId": null,
        "ended": false,
        "suspended": false,
        "tenantId": null
    }
]
```

**What We Extract:**
- `id` - Process instance ID (used for further processing)

## 🧪 Testing

### Test CSV File

Create a test CSV file (`test_report.csv`):

```csv
Camunda Business Key,Camunda Client Code,Camunda Contract Number,Camunda Start Time,OnBase Business Key,OnBase ClientCode,OnBase CaseQueue,OnBase ContractNumber,Match,Document Processing Date,Aging (In Days),OnBase PendingCallOut,OnBase Notes
65bbefb7-be4d-11f0-b75b-0219df3ccdf7,CLIENT001,7280001036,2025-11-10 10:30:00,65bbefb7-be4d-11f0-b75b-0219df3ccdf7,CLIENT001,TQA - New,7280001036,Not Matching,11/10/2025,3,No,Test case
BKEY456,CLIENT002,7280001037,2025-11-09 14:20:00,,CLIENT002,TQA - Complete,7280001037,Matching,11/09/2025,2,No,Should be filtered out
```

### Test Steps

1. Start backend: `mvn spring-boot:run`
2. Start frontend: `npm run dev`
3. Open `http://localhost:3000`
4. Click "Daily Report Monitoring" tab
5. Upload `test_report.csv`
6. Click "Upload & Process"
7. Verify results

### Expected Results

- Total Rows: 2
- Not Matching: 1
- Business Keys: 1 (`65bbefb7-be4d-11f0-b75b-0219df3ccdf7`)
- Processed Cases: Will depend on Camunda response

## 📊 Comparison: Data Entry vs Daily Report

| Aspect | Data Entry Waiting Cases | Daily Report |
|--------|-------------------------|--------------|
| **Trigger** | Button click | CSV file upload |
| **Source** | Camunda execution API | CSV file |
| **Initial Data** | Process instance IDs | Business keys |
| **First Step** | Direct to Camunda variables | Camunda → Get instances from business key |
| **Processing** | Same | Same (after getting instance IDs) |
| **Output** | Processing results | Processing results + CSV stats |

## 🎯 Business Key Priority

When extracting business keys from CSV:

```java
public String getBusinessKeyToUse() {
    // Priority 1: OnBase Business Key
    if (onBaseBusinessKey != null && !onBaseBusinessKey.trim().isEmpty()) {
        return onBaseBusinessKey;
    }
    // Priority 2: Camunda Business Key (fallback)
    return camundaBusinessKey;
}
```

## 📈 Statistics Displayed

**CSV Statistics:**
- Total rows in CSV
- Not matching rows found
- Business keys extracted (unique)
- Cases processed

**Processing Statistics:**
- Successful cases
- Failed cases
- Manual review required

**Document Collections:**
- To Cancel count
- To Returning count
- To Complete count
- Manual Review count

## 🔐 File Validation

**Size Limit:** 10MB
**File Type:** CSV only (.csv extension)
**Encoding:** UTF-8

**Validation Checks:**
1. File not empty
2. File extension is .csv
3. File size < 10MB
4. Valid CSV format

## 📚 Related Features

- **Data Entry Waiting Cases** - Process cases from Camunda execution API
- **Case Categorization** - Based on OnBase status and BPM Follow-Up tasks
- **MongoDB Integration** - Check caseStatus and staleness
- **Business Days Calculation** - Exclude weekends for stale detection

---

**Created:** November 10, 2025
**Version:** 2.0.0 (Added Daily Report Processing)




