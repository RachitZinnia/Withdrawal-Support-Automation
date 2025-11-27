# Withdrawal Support Automation

A comprehensive support automation system for daily case monitoring and processing of data entry waiting cases.

## 🎯 Overview

This application automates the monitoring and processing of withdrawal support cases through:

1. **Data Entry API Integration** - Fetches all waiting cases
2. **Case Details Retrieval** - Gets case IDs and client variables
3. **OnBase Integration** - Retrieves case status and documents from OnBase
4. **Automated Actions** - Takes appropriate actions based on OnBase status
5. **MongoDB Verification** - Checks case status in MongoDB
6. **Manual Review Flagging** - Identifies cases requiring manual attention
7. **Stale Case Detection** - Flags cases in progress > 2 days

## 🏗️ Architecture

- **Backend**: Java Spring Boot (REST API)
- **Frontend**: React with Vite
- **Database**: MongoDB
- **External APIs**: Data Entry API, Case Details API, OnBase API

## 📁 Project Structure

```
Withdrawal Support Automation/
├── backend/                    # Spring Boot application
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/withdrawal/support/
│   │       │       ├── config/          # Configuration classes
│   │       │       ├── controller/      # REST controllers
│   │       │       ├── dto/             # Data Transfer Objects
│   │       │       ├── model/           # Domain models
│   │       │       ├── repository/      # MongoDB repositories
│   │       │       └── service/         # Business logic services
│   │       └── resources/
│   │           └── application.properties
│   └── pom.xml                 # Maven dependencies
│
└── frontend/                   # React application
    ├── src/
    │   ├── components/         # React components
    │   ├── App.jsx            # Main app component
    │   └── main.jsx           # Entry point
    ├── package.json
    └── vite.config.js
```

## 🚀 Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- Node.js 18+ and npm
- MongoDB 6.0+

### Backend Setup

1. Navigate to the backend directory:
```bash
cd backend
```

2. Configure your environment:
```bash
cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
```

3. Edit `application-local.properties` with your actual API credentials and MongoDB connection details.

4. Build and run the application:
```bash
mvn clean install
mvn spring-boot:run
```

The backend API will be available at `http://localhost:8080`

### Frontend Setup

1. Navigate to the frontend directory:
```bash
cd frontend
```

2. Install dependencies:
```bash
npm install
```

3. Start the development server:
```bash
npm run dev
```

The frontend will be available at `http://localhost:3000`

## 🔌 API Endpoints

### Main Processing Endpoint

**POST** `/api/cases/process-dataentry-waiting`

Processes all data entry waiting cases through the complete workflow.

**Response:**
```json
{
  "totalCases": 100,
  "successfulCases": 85,
  "failedCases": 5,
  "manualReviewRequired": 10,
  "message": "Processing completed successfully",
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

**GET** `/api/cases/health`

Returns service health status.

## 🎨 Features

### Backend Features
- ✅ RESTful API architecture
- ✅ External API integration (Data Entry, OnBase, MongoDB)
- ✅ Automated case processing workflow
- ✅ Business rule engine
- ✅ Comprehensive error handling
- ✅ Logging and monitoring
- ✅ Configurable thresholds

### Frontend Features
- ✅ Modern, responsive UI design
- ✅ One-click case processing
- ✅ Real-time processing feedback
- ✅ Comprehensive results dashboard
- ✅ Detailed case-by-case breakdown
- ✅ Visual status indicators
- ✅ Manual review flagging

## 🔧 Configuration

### Backend Configuration

Key configuration properties in `application.properties`:

```properties
# Server
server.port=8080

# MongoDB
spring.data.mongodb.uri=mongodb://localhost:27017/withdrawal_support

# External APIs
api.dataentry.url=https://your-data-entry-api.com
api.dataentry.key=your_api_key

# Business Rules
business.days-threshold=2
```

### Environment Variables

You can override any property using environment variables:

```bash
export MONGODB_URI=mongodb://prod-server:27017/withdrawal_support
export DATA_ENTRY_API_KEY=your_production_key
export DAYS_THRESHOLD=3
```

## 📊 Case Processing Workflow

```
1. Fetch Data Entry Waiting Cases
        ↓
2. For each case:
   - Get Case Details (ID + Client Variables)
        ↓
3. Query OnBase for Case Information
        ↓
4. Take Action Based on OnBase Status
   - PENDING → Check MongoDB
   - PROCESSING → Update status
   - COMPLETED → Finalize
   - MISSING_DOCS → Request documents
   - REJECTED → Notify
        ↓
5. For PENDING cases in OnBase:
   - Check MongoDB status
   - If NOT in progress → Start processing
   - If in progress → Check age
        ↓
6. Flag cases in progress > 2 days for manual review
```

## 🎯 Case Status Types

| Status | Description |
|--------|-------------|
| `PENDING` | Case awaiting processing |
| `IN_PROGRESS` | Case currently being processed |
| `COMPLETED` | Case successfully completed |
| `FAILED` | Case processing failed |
| `MANUAL_REVIEW_REQUIRED` | Case requires manual intervention |

## 🛠️ Development

### Building for Production

**Backend:**
```bash
cd backend
mvn clean package
java -jar target/withdrawal-support-automation-1.0.0.jar
```

**Frontend:**
```bash
cd frontend
npm run build
npm run preview
```

## 📝 License

This project is proprietary software.

## 👥 Support

For issues or questions, please contact the development team.





