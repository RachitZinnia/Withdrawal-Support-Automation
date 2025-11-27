# Camunda BPM Integration

This document explains how the Withdrawal Support Automation system integrates with Camunda BPM.

## 📡 Camunda API Endpoints

### 1. Data Entry Waiting Cases

**Endpoint:** `GET https://bpm.se2.com/engine-rest/execution`

**Query Parameters:**
- `processDefinitionKey=dataentry`
- `activityId=Event_0a7e4e6`
- `active=true`

**Full URL:**
```
https://bpm.se2.com/engine-rest/execution?processDefinitionKey=dataentry&activityId=Event_0a7e4e6&active=true
```

### 2. Process Instance Variables

**Endpoint:** `GET https://bpm.se2.com/engine-rest/process-instance/{process_instance_id}/variables/{variable_name}`

**Path Parameters:**
- `{process_instance_id}` - The process instance ID
- `{variable_name}` - The name of the variable to retrieve

**Variables Retrieved:**
- `clientCode` - Client code for the case
- `onbaseCaseId` - OnBase case ID for document retrieval

**Example URLs:**
```
https://bpm.se2.com/engine-rest/process-instance/88205faa-bc6f-11f0-a49b-02c29394f0e3/variables/clientCode
https://bpm.se2.com/engine-rest/process-instance/88205faa-bc6f-11f0-a49b-02c29394f0e3/variables/onbaseCaseId
```

### Response Format

The API returns a JSON array of execution objects:

```json
[
    {
        "id": "0658174d-bc74-11f0-8a98-0ef344296861",
        "processInstanceId": "88205faa-bc6f-11f0-a49b-02c29394f0e3",
        "ended": false,
        "tenantId": null
    },
    {
        "id": "06588c83-bc74-11f0-8a98-0ef344296861",
        "processInstanceId": "9002a6e2-bc6f-11f0-a49b-02c29394f0e3",
        "ended": false,
        "tenantId": null
    }
]
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Execution ID (unique identifier for this execution) |
| `processInstanceId` | String | Process Instance ID (used as case reference) |
| `ended` | Boolean | Whether the execution has ended |
| `tenantId` | String | Tenant ID (if multi-tenancy is enabled) |

### Variable Response Format

When fetching variables, the API returns:

```json
{
    "type": "String",
    "value": "191902213",
    "valueInfo": {}
}
```

**Response Fields:**
| Field | Type | Description |
|-------|------|-------------|
| `type` | String | Data type of the variable (String, Integer, Boolean, etc.) |
| `value` | String | The actual value of the variable |
| `valueInfo` | Object | Additional metadata about the variable |

## 🔧 Backend Implementation

### 1. DTOs (Data Transfer Objects)

**File:** `backend/src/main/java/com/withdrawal/support/dto/DataEntryCase.java`

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataEntryCase {
    private String id;                    // Execution ID
    private String processInstanceId;     // Process Instance ID (used as case reference)
    private Boolean ended;
    private String tenantId;
}
```

**File:** `backend/src/main/java/com/withdrawal/support/dto/CamundaVariable.java`

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CamundaVariable {
    private String type;                  // Variable type
    private String value;                 // Variable value
    private Map<String, Object> valueInfo; // Additional metadata
}
```

**File:** `backend/src/main/java/com/withdrawal/support/dto/CaseDetails.java`

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseDetails {
    private String caseId;               // OnBase Case ID (from Camunda variable)
    private String caseReference;        // Process Instance ID
    private String clientCode;           // Client Code (from Camunda variable)
    private Map<String, Object> clientVariables;
    // ... other fields
}
```

### 2. Service Layer

**File:** `backend/src/main/java/com/withdrawal/support/service/DataEntryService.java`

#### Fetching Waiting Cases

```java
public List<DataEntryCase> getDataEntryWaitingCases() {
    log.info("Fetching data entry waiting cases from Camunda BPM");
    
    WebClient webClient = webClientBuilder
            .baseUrl(apiConfig.getDataentry().getUrl())
            .build();

    List<DataEntryCase> cases = webClient.get()
            .uri(uriBuilder -> uriBuilder
                    .path("/execution")
                    .queryParam("processDefinitionKey", "dataentry")
                    .queryParam("activityId", "Event_0a7e4e6")
                    .queryParam("active", "true")
                    .build())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<DataEntryCase>>() {})
            .block();

    return cases != null ? cases : List.of();
}
```

#### Fetching Case Details (Variables)

```java
public CaseDetails getCaseDetails(String processInstanceId) {
    // Get clientCode variable
    String clientCode = getCamundaVariable(processInstanceId, "clientCode");
    
    // Get onbaseCaseId variable
    String onbaseCaseId = getCamundaVariable(processInstanceId, "onbaseCaseId");
    
    // Build client variables map
    Map<String, Object> clientVariables = new HashMap<>();
    clientVariables.put("clientCode", clientCode);
    clientVariables.put("processInstanceId", processInstanceId);
    
    // Build and return case details
    return CaseDetails.builder()
            .caseId(onbaseCaseId)
            .caseReference(processInstanceId)
            .clientCode(clientCode)
            .clientVariables(clientVariables)
            .build();
}

private String getCamundaVariable(String processInstanceId, String variableName) {
    WebClient webClient = webClientBuilder
            .baseUrl(apiConfig.getDataentry().getUrl())
            .build();

    CamundaVariable variable = webClient.get()
            .uri("/process-instance/{processInstanceId}/variables/{variableName}", 
                    processInstanceId, variableName)
            .retrieve()
            .bodyToMono(CamundaVariable.class)
            .block();

    return (variable != null && variable.getValue() != null) ? variable.getValue() : null;
}
```

### 3. Configuration

**File:** `backend/src/main/resources/application.properties`

```properties
# Data Entry API Configuration (Camunda BPM)
api.dataentry.url=${DATA_ENTRY_API_URL:https://bpm.se2.com/engine-rest}
api.dataentry.key=${DATA_ENTRY_API_KEY:your_api_key}
```

## 📊 Data Flow

```
1. Application calls DataEntryService.getDataEntryWaitingCases()
           ↓
2. Service constructs Camunda REST API call:
   GET https://bpm.se2.com/engine-rest/execution
   ?processDefinitionKey=dataentry
   &activityId=Event_0a7e4e6
   &active=true
           ↓
3. Camunda returns list of executions (each with processInstanceId)
           ↓
4. For each processInstanceId, fetch variables:
           ↓
5a. GET /process-instance/{id}/variables/clientCode
    Returns: { "type": "String", "value": "CLIENT123", "valueInfo": {} }
           ↓
5b. GET /process-instance/{id}/variables/onbaseCaseId
    Returns: { "type": "String", "value": "191902213", "valueInfo": {} }
           ↓
6. Build CaseDetails object:
   - caseId = onbaseCaseId (e.g., "191902213")
   - caseReference = processInstanceId
   - clientCode = clientCode
   - clientVariables = { clientCode, processInstanceId }
           ↓
7. Use caseId (OnBase Case ID) to query OnBase
           ↓
8. Use clientCode for any client-specific operations
           ↓
9. Case processing continues with OnBase and MongoDB
```

## 🔄 Process Flow

### Understanding the Process

1. **Process Definition Key**: `dataentry`
   - Identifies the specific BPMN process
   - In this case, the data entry workflow

2. **Activity ID**: `Event_0a7e4e6`
   - Identifies the specific point in the process
   - This is the "waiting" event where cases pause

3. **Active**: `true`
   - Only returns currently active executions
   - Filters out completed or cancelled instances

### Case Identification

The **`processInstanceId`** is the unique identifier for each case and is used throughout the system as the case reference. This ID is passed to:

- Case Details API (to get case information)
- OnBase API (to get documents and status)
- MongoDB (to track case progress)

## 🛠️ Configuration Setup

### Local Development

1. Create `application-local.properties`:
```bash
cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
```

2. Update the Camunda configuration:
```properties
# Data Entry API (Camunda BPM)
api.dataentry.url=https://bpm.se2.com/engine-rest
api.dataentry.key=YOUR_ACTUAL_API_KEY  # If authentication is required
```

### Environment Variables

You can also use environment variables:

```bash
export DATA_ENTRY_API_URL=https://bpm.se2.com/engine-rest
export DATA_ENTRY_API_KEY=your_api_key
```

## 🧪 Testing the Integration

### Test with cURL

```bash
# Test Camunda endpoint directly
curl -X GET "https://bpm.se2.com/engine-rest/execution?processDefinitionKey=dataentry&activityId=Event_0a7e4e6&active=true"
```

### Test with Application

1. Start the backend:
```bash
cd backend
mvn spring-boot:run
```

2. Call the processing endpoint:
```bash
curl -X POST http://localhost:8080/api/cases/process-dataentry-waiting
```

3. Check the logs:
```
[INFO] Fetching data entry waiting cases from Camunda BPM
[INFO] Retrieved X waiting cases from Camunda
```

## 🔐 Authentication

If the Camunda API requires authentication:

### Bearer Token
The service is configured to send a Bearer token if `api.dataentry.key` is provided:

```java
.defaultHeader("Authorization", "Bearer " + apiConfig.getDataentry().getKey())
```

### Basic Auth
If you need Basic Authentication instead, update the `DataEntryService`:

```java
webClient.get()
    .uri(...)
    .headers(headers -> headers.setBasicAuth(username, password))
    .retrieve()
    ...
```

## 📝 Important Notes

### Process Instance ID vs Execution ID

- **Execution ID (`id`)**: Unique identifier for this specific execution/activity
- **Process Instance ID (`processInstanceId`)**: Unique identifier for the entire process instance

We use the **processInstanceId** as the case reference because:
- It remains constant throughout the case lifecycle
- It can be used to query the full process history
- It links to all related executions and activities

### Filtering Active Executions

The `active=true` parameter ensures we only get cases that are:
- Currently waiting at the specified activity
- Not yet completed
- Not cancelled or terminated

### Activity ID

The `activityId=Event_0a7e4e6` is specific to your BPMN process definition. This ID:
- Identifies the exact waiting point in the process
- May be different in different environments (dev, staging, prod)
- Can be found in the BPMN XML or Camunda Modeler

## 🔍 Troubleshooting

### No Cases Returned

If the API returns an empty array `[]`:
- Check if there are actually cases waiting at that activity
- Verify the `processDefinitionKey` is correct
- Verify the `activityId` matches your BPMN
- Check if `active=true` is appropriate

### Connection Errors

If you get connection errors:
- Verify the URL is accessible: `https://bpm.se2.com/engine-rest`
- Check network connectivity
- Verify firewall rules
- Check VPN requirements

### Authentication Errors

If you get 401/403 errors:
- Verify API key is correct
- Check if token has expired
- Verify authentication method (Bearer vs Basic)
- Check if API requires additional headers

## 🚀 Next Steps

After fetching the waiting cases:

1. **Get Case Details**: Use `processInstanceId` to fetch detailed case information
2. **Check OnBase**: Query OnBase for document status
3. **Verify MongoDB**: Check case history and status
4. **Take Action**: Process based on current status
5. **Update Records**: Save processing results

## 📚 Additional Resources

- [Camunda REST API Documentation](https://docs.camunda.org/manual/latest/reference/rest/)
- [Execution REST API](https://docs.camunda.org/manual/latest/reference/rest/execution/)
- [Process Instance REST API](https://docs.camunda.org/manual/latest/reference/rest/process-instance/)

---

**Last Updated**: November 10, 2025
**Integration Version**: 1.0.0

