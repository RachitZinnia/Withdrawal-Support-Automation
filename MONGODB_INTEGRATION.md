# MongoDB Integration

This document explains how the system integrates with MongoDB's ZLCM database to check case status.

## 🔌 MongoDB Connection

**Connection String:**
```
mongodb+srv://otp-reporting-prod:YpdwwzafSgNYkNTo@otp-prod-pl-0.0k1fz.mongodb.net/?retryWrites=true&w=majority
```

**Database:** `zlcm`

**Collection:** `case_instance`

## 📊 Query Structure

**Query Format:**
```javascript
{ "identifiers.value": "20251020-EM-267711" }
```

**Field Explanation:**
- `identifiers` - Array of identifier objects
- `identifiers.value` - The document number from OnBase

## 🔄 Data Flow

```
1. Get OnBase case details
   ↓
2. Extract documentNumber (e.g., "20251020-EM-267711")
   ↓
3. Query MongoDB case_instance collection:
   { "identifiers.value": "20251020-EM-267711" }
   ↓
4. Check status field in response:
   ├─ "in progress" / "in_progress" / "inprogress" → Case IS in progress
   └─ Any other value or not found → Case is NOT in progress
   ↓
5. Make decision:
   ├─ NOT in progress → Can process case
   └─ In progress → Skip, already being handled
```

## 📝 Document Structure

**MongoDB Document Example:**
```json
{
    "_id": "507f1f77bcf86cd799439011",
    "identifiers": [
        {
            "type": "documentNumber",
            "value": "20251020-EM-267711"
        }
    ],
    "status": "in progress",
    "createdDate": "2025-10-20T10:30:00Z",
    "lastUpdated": "2025-10-20T15:45:00Z",
    "metadata": {
        "assignedTo": "user123",
        "priority": "high"
    }
}
```

## 🏗️ Backend Implementation

### Model

**CaseInstanceDocument.java**
```java
@Document(collection = "case_instance")
public class CaseInstanceDocument {
    @Id
    private String id;
    private List<Identifier> identifiers;
    private String status;
    private LocalDateTime createdDate;
    private LocalDateTime lastUpdated;
    
    @Data
    public static class Identifier {
        private String type;
        private String value;  // This is the document number
    }
}
```

### Repository

**CaseInstanceRepository.java**
```java
@Repository
public interface CaseInstanceRepository extends MongoRepository<CaseInstanceDocument, String> {
    
    @Query("{ 'identifiers.value': ?0 }")
    Optional<CaseInstanceDocument> findByDocumentNumber(String documentNumber);
}
```

### Service

**CaseMongoService.java**
```java
public boolean isNotInProgress(String documentNumber) {
    Optional<CaseInstanceDocument> caseInstance = 
        caseInstanceRepository.findByDocumentNumber(documentNumber);
    
    if (caseInstance.isEmpty()) {
        return true; // Not found = not in progress
    }
    
    String status = caseInstance.get().getStatus();
    boolean isInProgress = "in progress".equalsIgnoreCase(status) || 
                          "in_progress".equalsIgnoreCase(status) ||
                          "inprogress".equalsIgnoreCase(status);
    
    return !isInProgress;
}
```

## 🎯 Use Cases

### Use Case 1: Case Found and In Progress

**MongoDB Document:**
```json
{
    "identifiers": [{ "value": "20251020-EM-267711" }],
    "status": "in progress"
}
```

**Result:**
- `isNotInProgress()` returns `false`
- Case is skipped (already being processed)
- Message: "Already in progress in MongoDB"

### Use Case 2: Case Found but NOT In Progress

**MongoDB Document:**
```json
{
    "identifiers": [{ "value": "20251020-EM-267711" }],
    "status": "pending"
}
```

**Result:**
- `isNotInProgress()` returns `true`
- Case can be processed
- Message: "Not in progress in MongoDB"

### Use Case 3: Case Not Found

**MongoDB Query Result:** Empty

**Result:**
- `isNotInProgress()` returns `true`
- Case can be processed (new case)
- Message: "Not in progress in MongoDB"

## 📊 Integration Flow

```
┌────────────────────────────────────────────┐
│  1. Camunda: Get waiting cases             │
└────────────────┬───────────────────────────┘
                 │
┌────────────────▼───────────────────────────┐
│  2. Camunda: Get clientCode & onbaseCaseId │
└────────────────┬───────────────────────────┘
                 │
┌────────────────▼───────────────────────────┐
│  3. OnBase: Get case details               │
│     - Includes documentNumber               │
└────────────────┬───────────────────────────┘
                 │
┌────────────────▼───────────────────────────┐
│  4. Categorize case                        │
│     → If CHECK_MONGODB category:           │
└────────────────┬───────────────────────────┘
                 │
┌────────────────▼───────────────────────────┐
│  5. MongoDB: Query case_instance           │
│     { "identifiers.value": documentNumber }│
└────────────────┬───────────────────────────┘
                 │
         ┌───────┴───────┐
         │               │
    Found & In Progress  │  Not Found OR Not In Progress
         │               │
         ▼               ▼
  ┌──────────┐    ┌──────────┐
  │   SKIP   │    │ PROCESS  │
  │   Case   │    │   Case   │
  └──────────┘    └──────────┘
```

## 🔧 Configuration

**application.properties**
```properties
# MongoDB Configuration
spring.data.mongodb.uri=mongodb+srv://otp-reporting-prod:YpdwwzafSgNYkNTo@otp-prod-pl-0.0k1fz.mongodb.net/?retryWrites=true&w=majority
spring.data.mongodb.database=zlcm
```

## 🧪 Testing

### Test MongoDB Connection

```bash
# Using mongosh
mongosh "mongodb+srv://otp-reporting-prod:YpdwwzafSgNYkNTo@otp-prod-pl-0.0k1fz.mongodb.net/"

# Switch to zlcm database
use zlcm

# Test query
db.case_instance.findOne({ "identifiers.value": "20251020-EM-267711" })
```

### Test from Application

**Start the backend:**
```bash
cd backend
mvn spring-boot:run
```

**Check logs:**
```
[INFO] Checking MongoDB case_instance for document number: 20251020-EM-267711
[INFO] Case found in MongoDB - Status: in progress, Is in progress: true
```

## 📝 Status Values

The `status` field in MongoDB can have various values. The system checks for "in progress" variations:

| Status Value | Treated as In Progress? |
|-------------|------------------------|
| `"in progress"` | ✅ Yes |
| `"in_progress"` | ✅ Yes |
| `"inprogress"` | ✅ Yes |
| `"In Progress"` | ✅ Yes (case-insensitive) |
| `"pending"` | ❌ No |
| `"completed"` | ❌ No |
| `"new"` | ❌ No |
| `null` or not found | ❌ No |

## 🔍 Troubleshooting

### Case Not Found in MongoDB

**Possible Causes:**
- Document number doesn't exist in `case_instance` collection
- Incorrect collection name
- Database connection issues
- Document number format mismatch

**Solution:**
- Verify document number from OnBase matches MongoDB
- Check database and collection names
- Verify MongoDB connection string

### Connection Errors

**Error:** `MongoTimeoutException`

**Solution:**
- Check network connectivity
- Verify MongoDB cluster is accessible
- Check credentials are correct
- Ensure IP whitelist includes your server

### Query Returns Wrong Results

**Possible Causes:**
- `identifiers.value` field name changed
- Multiple identifiers with different types
- Document structure changed

**Solution:**
- Verify MongoDB document structure
- Check field names match exactly
- Test query directly in MongoDB shell

## 🔐 Security

**Credentials:**
- Username: `otp-reporting-prod`
- Password: `YpdwwzafSgNYkNTo` (encoded in connection string)
- Database: `zlcm`
- Collection: `case_instance` (read-only access recommended)

**Best Practices:**
- Use environment variables for credentials in production
- Implement read-only access for this application
- Monitor query performance
- Implement connection pooling

## 📊 Performance Considerations

**Index Recommendation:**
```javascript
// Create index on identifiers.value for faster queries
db.case_instance.createIndex({ "identifiers.value": 1 })
```

**Query Performance:**
- Single document query by indexed field: ~1-5ms
- Non-indexed query: ~100-500ms (depending on collection size)
- Recommended: Ensure index exists on `identifiers.value`

## 🚀 Future Enhancements

- [ ] Add caching for MongoDB queries
- [ ] Implement retry logic for failed queries
- [ ] Add bulk query support for multiple documents
- [ ] Implement change streams for real-time updates
- [ ] Add query performance monitoring

## 📚 Related Documentation

- `CAMUNDA_INTEGRATION.md` - Camunda API integration
- `ONBASE_INTEGRATION.md` - OnBase API integration
- `README.md` - System overview

---

**Last Updated:** November 10, 2025
**Version:** 1.4.0 (Added MongoDB Integration)





