# Withdrawal Support Automation - Backend

Java Spring Boot backend service for automated case processing.

## 🏗️ Technology Stack

- **Java**: 17
- **Spring Boot**: 3.1.5
- **Spring Data MongoDB**: For database operations
- **Spring WebFlux**: For reactive HTTP client
- **Lombok**: To reduce boilerplate code
- **Maven**: Dependency management

## 📦 Dependencies

Major dependencies (see `pom.xml` for complete list):

- `spring-boot-starter-web` - REST API support
- `spring-boot-starter-data-mongodb` - MongoDB integration
- `spring-boot-starter-webflux` - WebClient for external APIs
- `lombok` - Code generation
- `spring-boot-starter-validation` - Input validation

## 🚀 Quick Start

### 1. Prerequisites

- Java 17 or higher
- Maven 3.6+
- MongoDB running locally or remote connection

### 2. Configuration

Create `src/main/resources/application-local.properties`:

```properties
# MongoDB
spring.data.mongodb.uri=mongodb://localhost:27017/withdrawal_support

# Data Entry API
api.dataentry.url=https://your-data-entry-api.com
api.dataentry.key=your_api_key

# Case Details API
api.casedetails.url=https://your-case-details-api.com
api.casedetails.key=your_api_key

# OnBase API
api.onbase.url=https://your-onbase-api.com
api.onbase.key=your_api_key
api.onbase.username=your_username
api.onbase.password=your_password

# Business Rules
business.days-threshold=2
```

### 3. Build & Run

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run

# Or run the JAR
java -jar target/withdrawal-support-automation-1.0.0.jar
```

## 📡 API Documentation

### Process Data Entry Waiting Cases

**Endpoint:** `POST /api/cases/process-dataentry-waiting`

**Description:** Processes all data entry waiting cases through the complete workflow.

**Request:** No body required

**Response:**
```json
{
  "totalCases": 100,
  "successfulCases": 85,
  "failedCases": 5,
  "manualReviewRequired": 10,
  "message": "Processing completed: 100 total, 85 successful, 5 failed, 10 require manual review",
  "details": [
    {
      "caseReference": "REF123",
      "caseId": "CASE456",
      "status": "COMPLETED",
      "action": "PROCESS_PENDING",
      "message": "Case processed successfully",
      "processedAt": "2025-11-10T10:30:00",
      "requiresManualReview": false,
      "reviewReason": null
    }
  ]
}
```

### Health Check

**Endpoint:** `GET /api/cases/health`

**Response:**
```
Service is running
```

## 🏛️ Architecture

### Package Structure

```
com.withdrawal.support/
├── config/                 # Configuration classes
│   ├── ApiConfig.java     # External API configuration
│   ├── BusinessConfig.java # Business rules configuration
│   ├── WebClientConfig.java # HTTP client configuration
│   └── WebConfig.java     # CORS and web configuration
│
├── controller/            # REST controllers
│   └── CaseProcessingController.java
│
├── dto/                   # Data Transfer Objects
│   ├── CaseDetails.java
│   ├── CaseProcessingDetail.java
│   ├── DataEntryCase.java
│   ├── OnBaseCaseInfo.java
│   └── ProcessingResult.java
│
├── model/                 # Domain models
│   ├── CaseDocument.java  # MongoDB document
│   ├── CaseStatus.java    # Status enumeration
│   └── OnBaseStatus.java  # OnBase status enumeration
│
├── repository/            # Data access layer
│   └── CaseRepository.java
│
└── service/               # Business logic
    ├── CaseMongoService.java      # MongoDB operations
    ├── CaseProcessingService.java  # Main orchestration
    ├── DataEntryService.java       # Data Entry API client
    └── OnBaseService.java          # OnBase API client
```

### Service Layer

#### CaseProcessingService
Main orchestration service that coordinates the entire workflow:
- Fetches waiting cases
- Processes each case through the workflow
- Handles MongoDB status checks
- Identifies stale cases
- Aggregates results

#### DataEntryService
Handles communication with Data Entry API:
- `getDataEntryWaitingCases()` - Fetches all waiting cases
- `getCaseDetails()` - Gets case ID and client variables

#### OnBaseService
Manages OnBase API integration:
- `getOnBaseCaseInfo()` - Retrieves case information from OnBase
- `takeOnBaseAction()` - Executes actions on cases
- `determineActionForStatus()` - Maps status to actions

#### CaseMongoService
MongoDB data operations:
- `getCaseStatus()` - Retrieves case status
- `saveCase()` - Saves/updates cases
- `findStaleInProgressCases()` - Finds old in-progress cases
- `updateCaseStatus()` - Updates case status

## 🔐 Security Considerations

- API keys stored in properties files (excluded from version control)
- CORS configured for specific origins
- All external API calls include proper authentication headers

## 🧪 Testing

Run tests:
```bash
mvn test
```

## 📊 Logging

The application uses SLF4J with Logback for logging:

- `DEBUG` level for application logic
- `INFO` level for Spring framework
- Logs include timestamps, levels, and class names

View logs:
```bash
# Application logs
tail -f logs/application.log
```

## 🔧 Troubleshooting

### MongoDB Connection Issues
```bash
# Check MongoDB is running
mongosh --eval "db.adminCommand('ping')"

# Check connection string in properties file
```

### External API Connection Issues
- Verify API URLs are correct
- Check API keys are valid
- Ensure network connectivity
- Review logs for detailed error messages

### Build Issues
```bash
# Clean and rebuild
mvn clean install -U

# Skip tests if needed
mvn clean install -DskipTests
```

## 🚀 Deployment

### Production Build

```bash
mvn clean package -DskipTests
```

### Running in Production

```bash
java -jar target/withdrawal-support-automation-1.0.0.jar \
  --spring.profiles.active=prod \
  --spring.data.mongodb.uri=$MONGODB_URI \
  --api.dataentry.key=$DATA_ENTRY_API_KEY
```

### Docker Deployment (Optional)

Create a `Dockerfile`:
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

Build and run:
```bash
docker build -t withdrawal-support-backend .
docker run -p 8080:8080 withdrawal-support-backend
```

## 📈 Performance Considerations

- WebClient used for non-blocking HTTP calls
- MongoDB indexes on frequently queried fields
- Batch processing for multiple cases
- Configurable connection pools

## 🔄 Future Enhancements

- [ ] Add retry logic for failed API calls
- [ ] Implement circuit breaker pattern
- [ ] Add comprehensive unit and integration tests
- [ ] Implement async processing for large batches
- [ ] Add metrics and monitoring (Actuator)
- [ ] Implement caching for frequently accessed data





