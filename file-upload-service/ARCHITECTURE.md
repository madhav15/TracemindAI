# File Upload Service - Architecture Documentation

## Overview

The File Upload Service is a Spring Boot microservice that implements a clean, layered architecture for processing CSV file uploads. The service reads CSV files, validates their content, and persists the data to PostgreSQL with proper transaction management.

## Architecture Pattern

The service follows a **Layered (N-Tier) Architecture** pattern:

```
┌─────────────────────────────────────────┐
│           Presentation Layer            │
│    (REST Controller & Exception Handler)│
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│          Service Layer                  │
│  (Business Logic & Orchestration)       │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│        Persistence Layer                │
│  (Repositories & Data Access)           │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│         Database Layer                  │
│        (PostgreSQL)                     │
└─────────────────────────────────────────┘
```

## Package Structure

```
com.tracemindai.fileupload/
├── FileUploadServiceApplication.java      # Application entry point
├── controller/
│   └── FileUploadController.java           # REST endpoint handler
├── service/
│   └── FileUploadService.java              # Business logic
├── repository/
│   ├── JobRepository.java                  # Job data access
│   └── RecordRepository.java               # Record data access
├── entity/
│   ├── Job.java                            # Job JPA entity
│   └── Record.java                         # Record JPA entity
├── dto/
│   ├── FileUploadRequest.java              # Request DTO
│   ├── FileUploadResponse.java             # Response DTO
│   └── MemberRecord.java                   # CSV row DTO
├── exception/
│   ├── FileUploadException.java            # Custom exception
│   └── GlobalExceptionHandler.java         # Global error handler
└── util/
    ├── CsvParser.java                      # CSV parsing logic
    └── FileValidationUtil.java             # File validation utilities
```

## Component Details

### 1. Presentation Layer

#### FileUploadController
- **Responsibility**: Handle incoming HTTP requests and return responses
- **Key Endpoint**: `POST /api/jobs/upload`
- **Request Format**: `multipart/form-data` with file parameter
- **Response Format**: JSON with standardized ApiResponse wrapper
- **Error Handling**: Delegated to GlobalExceptionHandler
- **Logging**: Request reception and completion

```java
@PostMapping("/upload")
public ResponseEntity<ApiResponse<FileUploadResponse>> uploadCsv(
    @RequestParam("file") MultipartFile file
)
```

#### GlobalExceptionHandler
- **Responsibility**: Centralized exception handling for the service
- **Handled Exceptions**:
  - `FileUploadException` - Custom application exceptions
  - `MaxUploadSizeExceededException` - File size violations
  - `Exception` - Generic uncaught exceptions
- **Response**: Structured ErrorResponse with code, message, timestamp
- **Logging**: Error details with full stack trace

### 2. Service Layer

#### FileUploadService
- **Responsibility**: Orchestrate CSV processing workflow
- **Key Methods**:
  - `uploadCsv(MultipartFile)` - Main entry point
  - `createJob(String, int)` - Job record creation
  - `createRecords(Long, List<MemberRecord>)` - Record batch insertion
- **Transaction Management**: `@Transactional` ensures atomicity
- **Key Features**:
  - Delegates CSV parsing to CsvParser
  - Uses repositories for data persistence
  - Maintains separation of concerns
  - Provides structured logging

```java
@Transactional
public FileUploadResponse uploadCsv(MultipartFile file) {
    // Parse CSV
    List<MemberRecord> records = csvParser.parse(file);
    
    // Create Job
    Job job = createJob(file.getOriginalFilename(), records.size());
    
    // Create Records
    createRecords(job.getId(), records);
    
    // Return response
    return buildResponse(job);
}
```

### 3. Persistence Layer

#### Repositories
- **JobRepository** - Spring Data JPA for Job entity
- **RecordRepository** - Spring Data JPA for Record entity
- **Features**:
  - Automatic table management
  - CRUD operations
  - Query methods (extensible for future requirements)
  - Connection pooling via HikariCP

```java
@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
}

@Repository
public interface RecordRepository extends JpaRepository<Record, Long> {
}
```

### 4. Entity Layer

#### Job Entity
**Table**: `jobs`

```
┌─────────────────────────────────────┐
│ jobs                                │
├─────────────────────────────────────┤
│ id (PK)              : BIGINT        │
│ fileName             : VARCHAR(255)  │
│ totalRecords         : INTEGER       │
│ status               : VARCHAR(50)   │
│ createdAt (indexed)  : TIMESTAMP     │
│ updatedAt            : TIMESTAMP     │
└─────────────────────────────────────┘
```

**Key Features**:
- Auto-generated ID via `@GeneratedValue`
- Auto-populated timestamps via Hibernate annotations
- Immutable `createdAt` (updatable=false)
- Status tracking for job state

#### Record Entity
**Table**: `records`

```
┌──────────────────────────────────────┐
│ records                              │
├──────────────────────────────────────┤
│ id (PK)                 : BIGINT     │
│ jobId (FK)              : BIGINT     │
│ memberId                : VARCHAR... │
│ name                    : VARCHAR... │
│ mobile                  : VARCHAR... │
│ email                   : VARCHAR... │
│ communicationPreference : VARCHAR... │
│ status                  : VARCHAR(50)│
│ createdAt (indexed)     : TIMESTAMP  │
│ updatedAt               : TIMESTAMP  │
└──────────────────────────────────────┘
```

**Key Features**:
- References Job via jobId (simple FK relationship)
- All member data persisted as-is from CSV
- Status tracking for record state
- Audit timestamps for compliance

### 5. Utility Layer

#### CsvParser
- **Responsibility**: Parse and validate CSV files
- **Input**: MultipartFile (CSV format)
- **Output**: List<MemberRecord>
- **Workflow**:
  1. Validate file is not null/empty
  2. Verify .csv extension
  3. Read CSV with headers
  4. Parse each row into MemberRecord
  5. Skip invalid rows with logging
  6. Return valid records or throw exception
- **Error Handling**:
  - FileUploadException for validation failures
  - Graceful handling of malformed rows

```java
public List<MemberRecord> parse(MultipartFile file) {
    // Validation
    validateFile(file);
    
    // Parse with Apache Commons CSV
    try (CSVParser csvParser = createCsvParser(file)) {
        List<MemberRecord> records = new ArrayList<>();
        for (CSVRecord csvRecord : csvParser) {
            try {
                records.add(parseLine(csvRecord));
            } catch (IllegalArgumentException e) {
                // Skip invalid row and continue
            }
        }
        return records;
    }
}
```

#### FileValidationUtil
- **Responsibility**: File validation utilities
- **Methods**:
  - `isValidFileName(String)` - Check if name is not blank
  - `getFileExtension(String)` - Extract file extension
  - `isCsvFile(String)` - Verify .csv extension

## Data Flow Diagram

```
┌──────────────────┐
│  CSV File Upload │
└────────┬─────────┘
         │
         ▼
┌──────────────────────────────┐
│ FileUploadController         │
│ @PostMapping("/upload")      │
└────────┬─────────────────────┘
         │
         ▼
┌──────────────────────────────┐
│ FileUploadService            │
│ @Transactional uploadCsv()   │
└────────┬─────────────────────┘
         │
         ├─────────────────────────────┐
         │                             │
         ▼                             ▼
┌────────────────────┐      ┌────────────────────┐
│ CsvParser.parse()  │      │ JobRepository      │
│ • Validate file    │      │ • Create job       │
│ • Extract records  │      │ status=UPLOADED    │
└────────┬───────────┘      └────────────────────┘
         │                             │
         │                             │
         └─────────────┬───────────────┘
                       │
                       ▼
         ┌──────────────────────────────┐
         │ RecordRepository             │
         │ • Save all records           │
         │ status=RECEIVED              │
         └──────────────────────────────┘
                       │
                       ▼
         ┌──────────────────────────────┐
         │ PostgreSQL                   │
         │ • INSERT INTO jobs           │
         │ • INSERT INTO records (bulk) │
         └──────────────────────────────┘
                       │
                       ▼
         ┌──────────────────────────────┐
         │ FileUploadResponse           │
         │ {jobId, fileName, ...}       │
         └──────────────────────────────┘
```

## Transaction Management

### Transactional Boundary
```java
@Transactional
public FileUploadResponse uploadCsv(MultipartFile file) {
    // Everything inside is one database transaction
    csvParser.parse(file);      // No DB interaction
    jobRepository.save(job);    // INSERT
    recordRepository.saveAll(); // INSERT BATCH
}
// Auto-commit on success, rollback on exception
```

### ACID Compliance
- **Atomicity**: All-or-nothing (job + records inserted together)
- **Consistency**: Foreign key constraints maintained
- **Isolation**: Default READ_COMMITTED isolation level
- **Durability**: PostgreSQL WAL ensures durability

## Error Handling Strategy

### Exception Hierarchy
```
Exception
└── RuntimeException
    └── ApplicationException (from common-lib)
        └── FileUploadException
            ├── "File is empty or null"
            ├── "File must be a CSV file"
            ├── "CSV file contains no valid records"
            ├── "Error parsing CSV file"
```

### Error Response Format
```json
{
  "code": 400,
  "message": "Error description",
  "timestamp": "2026-06-13T10:30:45.123"
}
```

## HTTP Status Codes

| Code | Scenario |
|------|----------|
| 201 | Successful CSV upload |
| 400 | Invalid file/format |
| 413 | File size exceeded |
| 500 | Internal server error |

## Logging Strategy

### Log Levels
- **INFO**: Upload requests, job creation, record count
- **DEBUG**: CSV parsing details, field values
- **WARN**: Skipped invalid rows
- **ERROR**: Exceptions with full stack trace

### Key Log Points
```
INFO  - Received CSV upload request for file: members.csv
DEBUG - Successfully parsed 10 records from CSV file
INFO  - Created job with id: 1 for 10 records
INFO  - Created 10 record entries in database
INFO  - CSV upload completed successfully for jobId: 1
```

## Design Principles

### 1. Separation of Concerns
- Controllers handle HTTP only
- Services contain business logic
- Repositories manage data access
- Utilities provide reusable functions

### 2. Single Responsibility
- CsvParser: CSV parsing only
- FileUploadService: Orchestration only
- Repositories: CRUD operations only

### 3. Dependency Injection
- Constructor injection via Lombok @RequiredArgsConstructor
- Promotes testability and loose coupling
- Clear dependency declaration

### 4. Clean Code
- Meaningful variable/method names
- Short, focused methods
- No code duplication
- Type-safe operations

### 5. Production Ready
- Comprehensive logging
- Centralized exception handling
- Transaction management
- Configuration externalization
- Input validation

## Performance Considerations

### Database Optimization
- **Batch Inserts**: `recordRepository.saveAll(records)` uses batch mode
- **Connection Pooling**: HikariCP default (20 connections)
- **Indexes**: Automatic on PK and FK
- **Lazy Loading**: Not used (no relationships declared)

### CSV Parsing
- **Streaming**: Uses SAX-style parsing via Commons CSV
- **Memory Efficient**: Processes one row at a time
- **Large Files**: Can handle files limited only by heap size

### Network
- **Multipart Upload**: Efficient binary transmission
- **Response Compression**: Can be enabled in Spring
- **Timeout**: Configurable via server settings

## Scalability Considerations

### Horizontal Scaling
- Stateless service (can run multiple instances)
- Database acts as single source of truth
- Load balancer distributes requests

### Vertical Scaling
- Increase file size limits
- Adjust thread pool sizes
- Increase database connections

### Future Enhancements
- Async processing with message queue
- File storage on S3/GCS
- Record indexing for search
- Pagination APIs
- Batch validation rules

## Security Considerations

### Input Validation
- File extension verification
- File size limits
- Required field validation
- Charset validation (UTF-8)

### Database Security
- Parameterized queries (JPA handles this)
- Connection pooling with credentials in config
- No SQL injection possible

### API Security
- HTTPS ready (Spring Boot supports)
- CORS configurable
- Rate limiting ready (Spring addons)

## Testing Strategy

### Unit Testing (Future)
- CsvParser with valid/invalid CSVs
- FileUploadService with mocked repos
- Validation utilities

### Integration Testing (Future)
- End-to-end file upload flow
- Database transaction verification
- Error scenario handling

### Manual Testing
- cURL commands provided
- Sample CSV files included
- Verification queries documented

## Deployment Architecture

```
┌─────────────┐
│ Load        │
│ Balancer    │
└──────┬──────┘
       │
   ┌───┴────┬─────────┐
   │        │         │
   ▼        ▼         ▼
┌──────┐ ┌──────┐ ┌──────┐
│ FUS  │ │ FUS  │ │ FUS  │  (File Upload Service instances)
│ :8081│ │ :8081│ │ :8081│
└──┬───┘ └──┬───┘ └──┬───┘
   │        │         │
   └────────┼─────────┘
            │
       ┌────▼────┐
       │PostgreSQL│
       │Database  │
       └──────────┘
```

## Configuration Management

### Environment Variables
- `SPRING_DATASOURCE_URL` - Database connection
- `SPRING_DATASOURCE_USERNAME` - DB user
- `SPRING_DATASOURCE_PASSWORD` - DB password
- `SERVER_PORT` - Service port
- `LOGGING_LEVEL_COM_TRACEMINDAI` - Log level

### Property Files
- `application.yml` - Default configuration
- `application-prod.yml` - Production overrides
- `application-dev.yml` - Development overrides

## Monitoring & Observability

### Metrics (Future)
- Requests per second
- Average response time
- Error rate
- File processing duration

### Health Checks (Future)
- `/actuator/health` - Service health
- Database connectivity
- Disk space for uploads

### Alerting (Future)
- Error rate threshold
- Response time SLO
- Database connection pool exhaustion

## Conclusion

The File Upload Service demonstrates clean architecture principles through:
- Clear separation of layers
- Single responsibility per class
- Dependency injection for testability
- Comprehensive error handling
- Production-ready code quality
- Extensible design for future features

This foundation allows for easy testing, maintenance, and scaling as requirements evolve.
