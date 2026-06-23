# Resume Analyzer - Application Flow & Architecture

## Table of Contents
- [System Architecture](#system-architecture)
- [Data Flow](#data-flow)
- [Resume Processing Flow](#resume-processing-flow)
- [Candidate Matching Flow](#candidate-matching-flow)
- [Database Schema](#database-schema)
- [Frontend Architecture](#frontend-architecture)
- [API Communication](#api-communication)

---

## System Architecture

### High-Level Architecture

```mermaid
graph TB
    subgraph "Frontend Layer"
        UI[React App<br/>TypeScript + Redux]
        Redux[Redux Store<br/>State Management]
        Saga[Redux Saga<br/>Side Effects]
    end
    
    subgraph "API Layer"
        GQL[GraphQL API<br/>Query & Mutations]
        REST[REST API<br/>File Upload]
    end
    
    subgraph "Backend Services"
        Upload[File Upload<br/>Service]
        Parser[File Parser<br/>PDF/DOC/DOCX]
        Processor[Resume Processing<br/>Service @Async]
        AI[AI Service<br/>Spring AI]
        Embed[Embedding Service<br/>Vector Generation]
        Match[Candidate Matching<br/>Service]
    end
    
    subgraph "External Services"
        LLM[LM Studio<br/>Mistral 7B/LLaMA]
        EmbedModel[nomic-embed-text<br/>768-dim vectors]
    end
    
    subgraph "Data Layer"
        DB[(PostgreSQL<br/>+ pgvector)]
        Entities[JPA Entities<br/>5 Tables]
    end
    
    UI --> Redux
    Redux --> Saga
    Saga --> GQL
    Saga --> REST
    
    GQL --> Upload
    GQL --> Match
    REST --> Upload
    
    Upload --> Processor
    Processor --> Parser
    Processor --> AI
    Processor --> Embed
    Match --> AI
    
    AI --> LLM
    Embed --> EmbedModel
    
    Processor --> Entities
    Match --> Entities
    Entities --> DB
    
    style UI fill:#667eea,color:#fff
    style Redux fill:#764ba2,color:#fff
    style LLM fill:#48bb78,color:#fff
    style DB fill:#4299e1,color:#fff
```

### Technology Stack

| Layer | Technologies |
|-------|-------------|
| **Frontend** | React 18, TypeScript, Redux Toolkit, Redux-Saga, Vite |
| **API** | GraphQL (Spring GraphQL), REST (Spring MVC) |
| **Backend** | Java 25, Spring Boot 3.2.2, Spring AI |
| **AI/ML** | LM Studio, Mistral 7B, nomic-embed-text |
| **Database** | PostgreSQL 15+, pgvector extension |
| **Build** | Maven, frontend-maven-plugin, Yarn |

---

## Data Flow

### User Journey Overview

```mermaid
flowchart LR
    A[User] --> B{Action}
    B -->|Upload| C[File Upload]
    B -->|Search| D[Candidate Search]
    B -->|Create Job| E[Job Requirements]
    B -->|Match| F[AI Matching]
    
    C --> G[Process Resumes]
    G --> H[Extract Info]
    H --> I[Generate Embeddings]
    I --> J[(Database)]
    
    D --> J
    E --> J
    
    F --> K[AI Analysis]
    K --> L[Score Generation]
    L --> J
    J --> M[Display Results]
    M --> A
    
    style A fill:#667eea,color:#fff
    style J fill:#4299e1,color:#fff
    style K fill:#48bb78,color:#fff
```

---

## Resume Processing Flow

### Detailed Sequence Diagram

```mermaid
sequenceDiagram
    participant U as User
    participant UI as React UI
    participant REST as REST Controller
    participant FS as FileUploadService
    participant PS as ProcessingService
    participant FP as FileParserService
    participant AI as AIService
    participant ES as EmbeddingService
    participant LLM as LM Studio
    participant DB as PostgreSQL
    
    U->>UI: Upload Resume Files
    UI->>REST: POST /api/upload (multipart)
    REST->>FS: validateAndSave(files)
    FS->>DB: Create ProcessTracker (INITIATED)
    FS-->>REST: processId
    REST-->>UI: { processId, status }
    
    Note over PS: @Async Processing
    FS->>PS: processResumesAsync(files, processId)
    
    loop For Each File
        PS->>FP: extractText(file)
        FP->>FP: Detect format (PDF/DOC/DOCX)
        FP->>FP: Extract text using POI/PDFBox
        FP-->>PS: rawText
        
        PS->>AI: analyzeResume(text)
        AI->>LLM: POST /v1/chat/completions
        Note over LLM: Extract structured data<br/>(name, email, skills, etc.)
        LLM-->>AI: Candidate JSON
        AI-->>PS: candidateData
        
        PS->>DB: Save Candidate entity
        
        PS->>ES: generateAndStoreEmbeddings(candidateId, text)
        ES->>ES: Split text into chunks
        
        loop For Each Chunk
            ES->>LLM: POST /v1/embeddings
            LLM-->>ES: 768-dim vector
        end
        
        ES->>DB: Save ResumeEmbedding (with vector)
        DB-->>ES: Success
    end
    
    PS->>DB: Update ProcessTracker (COMPLETED)
    
    Note over UI: Polling every 2 seconds
    UI->>REST: GET /api/process/{id}/status
    REST->>DB: Find ProcessTracker
    DB-->>REST: { status, filesProcessed, ... }
    REST-->>UI: Process status
    UI->>UI: Update progress bar
```

### Process States

```mermaid
stateDiagram-v2
    [*] --> INITIATED: User uploads files
    INITIATED --> PARSING: Start processing
    PARSING --> AI_ANALYSIS: Text extracted
    AI_ANALYSIS --> EMBED_GENERATED: Candidate data saved
    EMBED_GENERATED --> COMPLETED: All files processed
    
    PARSING --> FAILED: Parse error
    AI_ANALYSIS --> FAILED: AI error
    EMBED_GENERATED --> FAILED: Embedding error
    
    COMPLETED --> [*]
    FAILED --> [*]
    
    note right of INITIATED
        ProcessTracker created
        Files validated
    end note
    
    note right of AI_ANALYSIS
        LLM extracts:
        - Name, Email, Mobile
        - Skills, Experience
        - Education, Summary
    end note
    
    note right of EMBED_GENERATED
        Vector embeddings stored
        for semantic search
    end note
```

---

## Candidate Matching Flow

### AI-Powered Matching Sequence

```mermaid
sequenceDiagram
    participant U as User
    participant UI as React UI
    participant GQL as GraphQL Resolver
    participant MS as MatchingService
    participant CR as CandidateRepo
    participant JR as JobRepo
    participant AI as AIService
    participant LLM as LM Studio
    participant DB as PostgreSQL
    
    U->>UI: Select Job & Click "Match All"
    UI->>GQL: matchAllCandidatesToJob(jobId)
    GQL->>MS: matchAllCandidates(jobId)
    
    MS->>JR: findById(jobId)
    JR->>DB: SELECT * FROM job_requirements
    DB-->>JR: Job data
    JR-->>MS: JobRequirement
    
    MS->>CR: findAll()
    CR->>DB: SELECT * FROM candidates
    DB-->>CR: List<Candidate>
    CR-->>MS: candidates[]
    
    loop For Each Candidate
        MS->>AI: matchCandidate(candidate, job)
        
        Note over AI: Build matching prompt with:<br/>- Job requirements<br/>- Candidate profile
        
        AI->>LLM: POST /v1/chat/completions
        Note over LLM: Analyze match quality<br/>Generate detailed scores
        LLM-->>AI: Scoring JSON
        AI-->>MS: MatchScores
        
        MS->>MS: Create CandidateMatch entity
        MS->>MS: Auto-shortlist if score >= 70
        MS->>DB: Save CandidateMatch
    end
    
    MS->>DB: Batch save all matches
    DB-->>MS: Success
    MS-->>GQL: List<CandidateMatch>
    GQL-->>UI: Match results
    
    UI->>UI: Display scores with breakdown
    U->>U: Review matches
```

### Scoring Breakdown

```mermaid
graph LR
    A[Candidate + Job] --> B[AI Analysis]
    
    B --> C[Skills Score<br/>0-100]
    B --> D[Experience Score<br/>0-100]
    B --> E[Education Score<br/>0-100]
    B --> F[Domain Score<br/>0-100]
    
    C --> G[Weighted Average]
    D --> G
    E --> G
    F --> G
    
    G --> H{Overall >= 70?}
    H -->|Yes| I[Auto Shortlist]
    H -->|No| J[Review Needed]
    
    I --> K[(Database)]
    J --> K
    
    style B fill:#48bb78,color:#fff
    style I fill:#667eea,color:#fff
    style K fill:#4299e1,color:#fff
```

---

## Database Schema

### Entity Relationship Diagram

```mermaid
erDiagram
    CANDIDATE ||--o{ RESUME_EMBEDDING : has
    CANDIDATE ||--o{ CANDIDATE_MATCH : participates
    JOB_REQUIREMENT ||--o{ CANDIDATE_MATCH : requires
    PROCESS_TRACKER ||--o| CANDIDATE : tracks
    
    CANDIDATE {
        uuid id PK
        string name
        string email
        string mobile
        string skills
        float experience
        string current_company
        string education
        text summary
        timestamp created_at
    }
    
    RESUME_EMBEDDING {
        uuid id PK
        uuid candidate_id FK
        text chunk_text
        vector_768 embedding "pgvector"
        int chunk_index
        timestamp created_at
    }
    
    JOB_REQUIREMENT {
        uuid id PK
        string title
        int min_experience
        int max_experience
        text required_skills
        string required_education
        text job_description
        boolean is_active
        timestamp created_at
    }
    
    CANDIDATE_MATCH {
        uuid id PK
        uuid candidate_id FK
        uuid job_id FK
        float overall_score
        float skills_score
        float experience_score
        float education_score
        float domain_score
        text remarks
        boolean is_shortlisted
        boolean is_selected
        timestamp matched_at
    }
    
    PROCESS_TRACKER {
        uuid id PK
        enum status "INITIATED|COMPLETED|FAILED"
        int total_files
        int files_processed
        int files_failed
        text error_message
        timestamp started_at
        timestamp completed_at
    }
```

### Database Indexes & Constraints

| Table | Index/Constraint | Type | Purpose |
|-------|-----------------|------|---------|
| **candidate** | `idx_candidate_name` | B-tree | Fast name search |
| **candidate** | `idx_candidate_email` | Unique | Prevent duplicates |
| **resume_embedding** | `idx_embedding_candidate` | B-tree | Join optimization |
| **resume_embedding** | `idx_embedding_vector` | ivfflat | Vector similarity search |
| **candidate_match** | `idx_match_job` | B-tree | Filter by job |
| **candidate_match** | `idx_match_score` | B-tree | Sort by score |
| **candidate_match** | `idx_match_shortlist` | B-tree | Filter shortlisted |

---

## Frontend Architecture

### Component Hierarchy

```mermaid
graph TB
    A[App.tsx] --> B[Router]
    B --> C[Layout]
    
    C --> D[Dashboard]
    C --> E[FileUpload]
    C --> F[CandidateList]
    C --> G[JobRequirements]
    C --> H[CandidateMatching]
    
    D --> I[StatsCard]
    D --> J[QuickActions]
    
    E --> K[DropZone]
    E --> L[FileList]
    E --> M[ProgressBar]
    
    F --> N[SearchBar]
    F --> O[CandidateCard]
    
    G --> P[JobCard]
    G --> Q[JobFormModal]
    
    H --> R[JobSelector]
    H --> S[MatchCard]
    H --> T[ScoreBreakdown]
    
    style A fill:#667eea,color:#fff
    style C fill:#764ba2,color:#fff
    style D fill:#48bb78,color:#fff
    style E fill:#48bb78,color:#fff
    style F fill:#48bb78,color:#fff
    style G fill:#48bb78,color:#fff
    style H fill:#48bb78,color:#fff
```

### Redux State Management

```mermaid
graph TB
    subgraph "Redux Store"
        A[Root Store]
        A --> B[candidatesSlice]
        A --> C[jobsSlice]
        A --> D[matchesSlice]
        A --> E[uploadSlice]
    end
    
    subgraph "State Shape"
        B --> B1[candidates: Candidate[]]
        B --> B2[loading: boolean]
        B --> B3[error: string]
        
        C --> C1[jobs: JobRequirement[]]
        C --> C2[selectedJob: Job]
        
        D --> D1[matches: CandidateMatch[]]
        D --> D2[matchingInProgress: boolean]
        
        E --> E1[tracker: ProcessTracker]
        E --> E2[uploading: boolean]
    end
    
    subgraph "Sagas (Side Effects)"
        F[Root Saga] --> G[fetchCandidatesSaga]
        F --> H[uploadFilesSaga]
        F --> I[matchCandidatesSaga]
        F --> J[createJobSaga]
    end
    
    G -.->|dispatch success| B
    H -.->|dispatch success| E
    I -.->|dispatch success| D
    J -.->|dispatch success| C
    
    style A fill:#667eea,color:#fff
    style F fill:#764ba2,color:#fff
```

### Data Flow in Frontend

```mermaid
flowchart LR
    A[User Action] --> B[Dispatch Action]
    B --> C{Async?}
    
    C -->|No| D[Reducer]
    D --> E[Update State]
    
    C -->|Yes| F[Saga Middleware]
    F --> G[API Call]
    G --> H{Success?}
    
    H -->|Yes| I[Dispatch Success]
    H -->|No| J[Dispatch Failure]
    
    I --> D
    J --> D
    
    E --> K[Re-render UI]
    
    style A fill:#667eea,color:#fff
    style F fill:#764ba2,color:#fff
    style G fill:#48bb78,color:#fff
    style E fill:#4299e1,color:#fff
```

---

## API Communication

### GraphQL Schema Overview

```mermaid
graph TB
    subgraph "Queries"
        Q1[candidate id]
        Q2[allCandidates]
        Q3[searchCandidatesByName name]
        Q4[searchCandidatesBySkill skill]
        Q5[allJobs]
        Q6[activeJobs]
        Q7[matchesForJob jobId limit]
        Q8[processStatus id]
    end
    
    subgraph "Mutations"
        M1[updateCandidate id input]
        M2[deleteCandidate id]
        M3[createJobRequirement input]
        M4[updateJobRequirement id input]
        M5[deleteJobRequirement id]
        M6[matchCandidateToJob candidateId jobId]
        M7[matchAllCandidatesToJob jobId]
        M8[updateMatchStatus matchId input]
    end
    
    subgraph "Types"
        T1[Candidate]
        T2[JobRequirement]
        T3[CandidateMatch]
        T4[ProcessTracker]
    end
    
    Q1 --> T1
    Q2 --> T1
    Q3 --> T1
    Q4 --> T1
    Q5 --> T2
    Q6 --> T2
    Q7 --> T3
    Q8 --> T4
    
    M1 --> T1
    M3 --> T2
    M4 --> T2
    M6 --> T3
    M7 --> T3
    M8 --> T3
    
    style Q1 fill:#48bb78,color:#fff
    style Q2 fill:#48bb78,color:#fff
    style M1 fill:#f6ad55,color:#fff
    style M3 fill:#f6ad55,color:#fff
```

### REST Endpoints

| Method | Endpoint | Purpose | Request | Response |
|--------|----------|---------|---------|----------|
| **POST** | `/api/upload` | Upload resume files | `multipart/form-data` | `{ processId, status }` |
| **GET** | `/api/process/{id}/status` | Get processing status | - | `ProcessTracker` |

### API Call Flow

```mermaid
sequenceDiagram
    participant C as Component
    participant S as Saga
    participant API as API Service
    participant BE as Backend
    
    C->>C: User clicks button
    C->>S: dispatch(action)
    
    alt GraphQL Request
        S->>API: graphqlClient.request(QUERY)
        API->>BE: POST /graphql
        BE-->>API: { data }
        API-->>S: Parsed data
    else REST Request
        S->>API: axios.post(url, formData)
        API->>BE: POST /api/upload
        BE-->>API: { response }
        API-->>S: Response data
    end
    
    S->>S: dispatch(success/failure)
    S->>C: State updated
    C->>C: Re-render with new data
```

---

## Deployment Architecture

### Recommended Production Setup

```mermaid
graph TB
    subgraph "Load Balancer"
        LB[Nginx / ALB]
    end
    
    subgraph "Application Tier"
        APP1[Spring Boot<br/>Instance 1]
        APP2[Spring Boot<br/>Instance 2]
        APP3[Spring Boot<br/>Instance 3]
    end
    
    subgraph "AI/ML Services"
        LLM[LM Studio<br/>GPU Instance]
        EMBED[Embedding Model<br/>Service]
    end
    
    subgraph "Data Tier"
        DB[(PostgreSQL<br/>Primary)]
        REPLICA[(PostgreSQL<br/>Read Replica)]
    end
    
    subgraph "Storage"
        S3[Object Storage<br/>Resume Files]
    end
    
    subgraph "Monitoring"
        PROM[Prometheus]
        GRAF[Grafana]
        LOGS[ELK Stack]
    end
    
    LB --> APP1
    LB --> APP2
    LB --> APP3
    
    APP1 --> LLM
    APP2 --> LLM
    APP3 --> LLM
    
    APP1 --> DB
    APP2 --> DB
    APP3 --> DB
    
    APP1 --> REPLICA
    APP2 --> REPLICA
    APP3 --> REPLICA
    
    APP1 --> S3
    APP2 --> S3
    APP3 --> S3
    
    APP1 --> PROM
    APP2 --> PROM
    APP3 --> PROM
    
    PROM --> GRAF
    APP1 --> LOGS
    APP2 --> LOGS
    APP3 --> LOGS
    
    style LB fill:#667eea,color:#fff
    style LLM fill:#48bb78,color:#fff
    style DB fill:#4299e1,color:#fff
    style PROM fill:#f6ad55,color:#fff
```

---

## Performance Considerations

### Optimization Strategies

1. **Database Optimization**
   - Connection pooling (HikariCP)
   - Query result caching
   - Batch inserts for embeddings
   - pgvector index tuning (ivfflat)

2. **Async Processing**
   - Resume processing runs asynchronously
   - Thread pool sizing: `@Async(TaskExecutor)`
   - Progress tracking via polling

3. **Frontend Optimization**
   - Code splitting with React.lazy()
   - Redux selector memoization
   - Debounced search inputs
   - Virtualized lists for large datasets

4. **AI Model Optimization**
   - Batched embedding generation
   - Model response caching
   - GPU acceleration for LLM
   - Text chunking optimization

### Scalability Diagram

```mermaid
graph LR
    A[Current: Single Instance] --> B{Scale?}
    B -->|Horizontal| C[Multiple App Instances]
    B -->|Vertical| D[Larger Instance]
    
    C --> E[Load Balancer]
    E --> F[Session Stickiness]
    
    C --> G[Shared Database]
    G --> H[Read Replicas]
    
    C --> I[Distributed Cache]
    I --> J[Redis Cluster]
    
    style A fill:#667eea,color:#fff
    style C fill:#48bb78,color:#fff
    style G fill:#4299e1,color:#fff
```

---

## Security Architecture

### Security Layers

```mermaid
graph TB
    A[User Request] --> B{Authentication}
    B -->|Failed| C[401 Unauthorized]
    B -->|Success| D{Authorization}
    
    D -->|Denied| E[403 Forbidden]
    D -->|Allowed| F[Rate Limiting]
    
    F -->|Exceeded| G[429 Too Many Requests]
    F -->|OK| H[Input Validation]
    
    H -->|Invalid| I[400 Bad Request]
    H -->|Valid| J[Business Logic]
    
    J --> K[Audit Logging]
    K --> L[Response]
    
    style B fill:#f6ad55,color:#fff
    style D fill:#f6ad55,color:#fff
    style J fill:#48bb78,color:#fff
```

### Recommended Security Measures

1. **Authentication & Authorization** (Future)
   - JWT tokens for API authentication
   - Role-based access control (RBAC)
   - OAuth2/OIDC integration

2. **Data Protection**
   - Encrypt sensitive candidate data
   - HTTPS/TLS for all connections
   - Secure file storage

3. **API Security**
   - Rate limiting per user/IP
   - CORS configuration
   - GraphQL query complexity limits
   - File upload validation

4. **Infrastructure**
   - Network segmentation
   - Firewall rules
   - Regular security patches
   - Secrets management (Vault)

---

## Summary

This Resume Analyzer application follows a modern, scalable architecture:

✅ **Microservices-ready** - Clear service boundaries  
✅ **AI-powered** - Local LLM integration via Spring AI  
✅ **Vector Search** - Semantic resume matching with pgvector  
✅ **Async Processing** - Non-blocking resume analysis  
✅ **Type-safe** - Full TypeScript frontend  
✅ **State Management** - Redux with Saga for side effects  
✅ **GraphQL API** - Efficient data fetching  
✅ **Production-ready** - Monitoring, logging, error handling  

### Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| **Spring AI over LangChain4J** | Better Spring Boot integration, simpler config |
| **GraphQL + REST** | GraphQL for queries, REST for file uploads |
| **Redux Saga** | Better control over async flows, testability |
| **pgvector** | Native PostgreSQL extension, no external service |
| **Local LLM** | Privacy, cost control, offline capability |
| **Async Processing** | Better UX, non-blocking operations |

---

**Document Version:** 1.0  
**Last Updated:** February 15, 2026  
**Maintained By:** Development Team
