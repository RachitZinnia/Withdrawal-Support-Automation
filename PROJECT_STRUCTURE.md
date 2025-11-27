# Project Structure

Complete overview of the Withdrawal Support Automation project structure.

## 📁 Directory Tree

```
Withdrawal Support Automation/
│
├── README.md                          # Main project documentation
├── QUICKSTART.md                      # Quick start guide
├── PROJECT_STRUCTURE.md               # This file
├── setup.sh                           # Automated setup script
├── .gitignore                         # Git ignore rules
│
├── backend/                           # Java Spring Boot Backend
│   ├── pom.xml                        # Maven configuration
│   ├── README.md                      # Backend documentation
│   ├── .gitignore                     # Backend-specific ignores
│   │
│   └── src/
│       └── main/
│           ├── java/com/withdrawal/support/
│           │   │
│           │   ├── WithdrawalSupportAutomationApplication.java  # Main application class
│           │   │
│           │   ├── config/                    # Configuration classes
│           │   │   ├── ApiConfig.java         # External API configuration
│           │   │   ├── BusinessConfig.java    # Business rules (days threshold)
│           │   │   ├── WebClientConfig.java   # HTTP client bean
│           │   │   └── WebConfig.java         # CORS configuration
│           │   │
│           │   ├── controller/                # REST API Controllers
│           │   │   └── CaseProcessingController.java  # Main endpoint
│           │   │
│           │   ├── dto/                       # Data Transfer Objects
│           │   │   ├── CaseDetails.java       # Case ID + client variables
│           │   │   ├── CaseProcessingDetail.java  # Individual case result
│           │   │   ├── DataEntryCase.java     # Waiting case data
│           │   │   ├── OnBaseCaseInfo.java    # OnBase case information
│           │   │   └── ProcessingResult.java  # Aggregated results
│           │   │
│           │   ├── model/                     # Domain Models
│           │   │   ├── CaseDocument.java      # MongoDB document model
│           │   │   ├── CaseStatus.java        # Status enumeration
│           │   │   └── OnBaseStatus.java      # OnBase status enum
│           │   │
│           │   ├── repository/                # Data Access Layer
│           │   │   └── CaseRepository.java    # MongoDB repository
│           │   │
│           │   └── service/                   # Business Logic
│           │       ├── CaseMongoService.java       # MongoDB operations
│           │       ├── CaseProcessingService.java  # Main orchestration
│           │       ├── DataEntryService.java       # Data Entry API client
│           │       └── OnBaseService.java          # OnBase API client
│           │
│           └── resources/
│               ├── application.properties                      # Main config
│               └── application-local.properties.example        # Config template
│
└── frontend/                          # React Frontend
    ├── package.json                   # NPM dependencies
    ├── vite.config.js                 # Vite configuration
    ├── index.html                     # HTML entry point
    ├── README.md                      # Frontend documentation
    ├── .gitignore                     # Frontend-specific ignores
    │
    └── src/
        ├── main.jsx                   # React entry point
        ├── App.jsx                    # Main app component
        ├── App.css                    # App styles
        ├── index.css                  # Global styles
        │
        └── components/
            ├── Header.jsx             # Application header
            ├── Header.css             # Header styles
            ├── CaseMonitoring.jsx     # Main case monitoring component
            └── CaseMonitoring.css     # Case monitoring styles
```

## 🎯 Component Responsibilities

### Backend Components

#### Configuration Layer (`config/`)
- **ApiConfig.java**: Manages external API endpoints and credentials
- **BusinessConfig.java**: Configurable business rules (e.g., days threshold)
- **WebClientConfig.java**: HTTP client configuration for API calls
- **WebConfig.java**: CORS settings for frontend integration

#### Controller Layer (`controller/`)
- **CaseProcessingController.java**: 
  - Main REST endpoint: `POST /api/cases/process-dataentry-waiting`
  - Health check endpoint: `GET /api/cases/health`

#### Service Layer (`service/`)
- **CaseProcessingService.java**: 
  - Orchestrates entire workflow
  - Aggregates results
  - Identifies stale cases
  
- **DataEntryService.java**: 
  - Fetches waiting cases
  - Retrieves case details
  
- **OnBaseService.java**: 
  - Gets case info from OnBase
  - Takes actions on cases
  - Maps statuses to actions
  
- **CaseMongoService.java**: 
  - MongoDB CRUD operations
  - Status queries
  - Stale case detection

#### Repository Layer (`repository/`)
- **CaseRepository.java**: 
  - Spring Data MongoDB interface
  - Custom query methods

#### Model Layer (`model/` & `dto/`)
- **Domain Models**: MongoDB entities
- **DTOs**: API request/response objects

### Frontend Components

#### Main Components (`src/`)
- **App.jsx**: Application root, routing
- **main.jsx**: React initialization

#### Feature Components (`src/components/`)
- **Header.jsx**: 
  - Branding
  - Application title
  - Visual identity
  
- **CaseMonitoring.jsx**: 
  - Process cases button
  - Loading states
  - Results display
  - Statistics cards
  - Case details table
  - Error handling

## 🔄 Data Flow

```
┌──────────────┐
│   Frontend   │
│  (React UI)  │
└──────┬───────┘
       │ HTTP POST
       ▼
┌──────────────────────────────┐
│  CaseProcessingController    │
│  /api/cases/process-...      │
└──────┬───────────────────────┘
       │
       ▼
┌──────────────────────────────┐
│  CaseProcessingService       │
│  (Main Orchestration)        │
└──┬───┬───┬──────────────────┬┘
   │   │   │                  │
   │   │   │                  │
   ▼   ▼   ▼                  ▼
┌────┐┌────┐┌──────┐    ┌──────────┐
│Data││Case││OnBase│    │ MongoDB  │
│Entry││Det.││      │    │ Service  │
│Svc ││Svc ││Svc   │    │          │
└────┘└────┘└──────┘    └──────────┘
   │     │      │              │
   ▼     ▼      ▼              ▼
┌─────────────────────────────────┐
│    External APIs + Database     │
└─────────────────────────────────┘
```

## 📊 Technology Stack Summary

### Backend Stack
| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 17+ | Programming language |
| Spring Boot | 3.1.5 | Application framework |
| Spring Data MongoDB | - | Database access |
| Spring WebFlux | - | Reactive HTTP client |
| Lombok | - | Reduce boilerplate |
| Maven | 3.6+ | Build tool |

### Frontend Stack
| Technology | Version | Purpose |
|------------|---------|---------|
| React | 18.2.0 | UI framework |
| Vite | 5.0.0 | Build tool |
| Axios | 1.6.0 | HTTP client |
| Lucide React | 0.292.0 | Icons |

### Database
| Technology | Version | Purpose |
|------------|---------|---------|
| MongoDB | 6.0+ | Document database |

## 🔌 API Integration Points

### External APIs
1. **Data Entry API**
   - Endpoint: `/waiting-cases`
   - Purpose: Get all data entry waiting cases

2. **Case Details API**
   - Endpoint: `/cases/{caseReference}`
   - Purpose: Get case ID and client variables

3. **OnBase API**
   - Endpoint: `/cases/info`
   - Purpose: Get case status and documents
   - Endpoint: `/cases/{caseId}/actions`
   - Purpose: Execute actions on cases

### Internal API
- **Backend REST API**
  - Base URL: `http://localhost:8080/api`
  - Endpoint: `POST /cases/process-dataentry-waiting`
  - Endpoint: `GET /cases/health`

## 📝 Configuration Files

### Backend Configuration
- `application.properties` - Main configuration
- `application-local.properties` - Local overrides (gitignored)
- `pom.xml` - Maven dependencies

### Frontend Configuration
- `package.json` - NPM dependencies
- `vite.config.js` - Build configuration
- `.env` (optional) - Environment variables

## 🔐 Security Considerations

### Gitignored Files
- `application-local.properties` (API keys)
- `.env` files (sensitive data)
- `node_modules/` (dependencies)
- `target/` (build artifacts)
- IDE configuration files

### CORS Configuration
- Allowed origins configured in `WebConfig.java`
- Default: `http://localhost:3000`

## 📦 Build Artifacts

### Backend
- **Build**: `mvn clean install`
- **Output**: `target/withdrawal-support-automation-1.0.0.jar`

### Frontend
- **Build**: `npm run build`
- **Output**: `dist/` directory (static files)

## 🎨 Styling Architecture

### CSS Organization
- Global styles: `index.css`
- Component styles: Co-located with components
- CSS Variables: Defined in `:root`
- Responsive breakpoints: Mobile-first approach

### Design System
- Colors: CSS custom properties
- Gradients: Linear gradients for modern look
- Shadows: Elevation system with 4 levels
- Typography: System fonts for performance

## 🚀 Development Workflow

1. **Start MongoDB**: Ensure database is running
2. **Start Backend**: `mvn spring-boot:run`
3. **Start Frontend**: `npm run dev`
4. **Development**: Hot reload enabled on both
5. **Testing**: Use browser and backend logs

## 📈 Scalability Considerations

### Backend
- Stateless design for horizontal scaling
- WebClient for non-blocking API calls
- MongoDB for flexible schema

### Frontend
- Component-based architecture
- Lazy loading capability
- Production build optimization

## 🔄 Future Extensibility

### Easy to Add
- New external API integrations
- Additional business rules
- New UI components
- More case statuses
- Additional filters/sorting
- Real-time updates (WebSocket)
- Authentication/authorization

### Extension Points
- Service layer: Add new services
- Controller layer: Add new endpoints
- Component layer: Add new UI features
- Repository layer: Add new queries
- Configuration: Add new properties

## 📚 Documentation Files

| File | Purpose |
|------|---------|
| `README.md` | Main project overview |
| `QUICKSTART.md` | Quick start guide |
| `PROJECT_STRUCTURE.md` | This file - structure documentation |
| `backend/README.md` | Backend-specific docs |
| `frontend/README.md` | Frontend-specific docs |
| `setup.sh` | Automated setup script |

## 🎓 Learning Path

### For Backend Developers
1. Start with `CaseProcessingController.java`
2. Follow to `CaseProcessingService.java`
3. Explore individual services
4. Review models and DTOs
5. Check configuration classes

### For Frontend Developers
1. Start with `App.jsx`
2. Explore `CaseMonitoring.jsx`
3. Review component styles
4. Check API integration in components
5. Explore global styles

### For Full-Stack Understanding
1. Read main `README.md`
2. Follow `QUICKSTART.md`
3. Trace a request from UI to database
4. Review this structure document
5. Explore both codebases

---

**Last Updated**: November 10, 2025
**Version**: 1.0.0





