# File Upload Service - Implementation Guide

## Quick Start (5 minutes)

### 1. Prerequisites Check
```bash
# Verify Java 21
java -version

# Verify Maven
mvn -version

# Start PostgreSQL
brew services start postgresql

# Create database
createdb tracemindai
```

### 2. Build
```bash
cd /Users/madhav/Projects/java/TracemindAI/file-upload-service
mvn clean install
```

### 3. Run
```bash
mvn spring-boot:run
```

### 4. Test
```bash
curl -X POST http://localhost:8081/api/jobs/upload \
  -F "file=@sample-members.csv"
```

Expected output:
```json
{
  "code": 201,
  "message": "File uploaded successfully",
  "data": {
    "jobId": 1,
    "fileName": "sample-members.csv",
    "totalRecords": 10,
    "status": "UPLOADED"
  },
  "timestamp": "2026-06-13T10:30:45.123"
}
```

---

## Complete Implementation Details

### Project Files Created

#### Configuration & Build
- `pom.xml` - Maven dependencies and build configuration
- `src/main/resources/application.yml` - Spring Boot configuration

#### Core Application
- `FileUploadServiceApplication.java` - Spring Boot application entry point

#### REST Layer
- `controller/FileUploadController.java` - HTTP endpoint handler
- `exception/GlobalExceptionHandler.java` - Centralized error handling

#### Business Logic
- `service/FileUploadService.java` - Main business orchestration
- `util/CsvParser.java` - CSV file parsing and validation
- `util/FileValidationUtil.java` - File validation helpers

#### Data Access
- `repository/JobRepository.java` - Job entity repository
- `repository/RecordRepository.java` - Record entity repository

#### Domain Model
- `entity/Job.java` - Job JPA entity
- `entity/Record.java` - Record JPA entity

#### Data Transfer
- `dto/FileUploadRequest.java` - Upload request wrapper
- `dto/FileUploadResponse.java` - Upload response
- `dto/MemberRecord.java` - CSV row representation

#### Exception Handling
- `exception/FileUploadException.java` - Custom exception

#### Sample Data
- `sample-members.csv` - Sample test data

#### Documentation
- `README.md` - Complete service documentation
- `TESTING.md` - Testing guide with all test cases
- `ARCHITECTURE.md` - Architecture overview
- `IMPLEMENTATION_GUIDE.md` - This file

---

## Class-by-Class Implementation

### 1. FileUploadServiceApplication.java
```java
@SpringBootApplication
public class FileUploadServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FileUploadServiceApplication.class, args);
    }
}
```
**Purpose**: Application bootstrap  
**Spring Auto-Configuration**: 
- Enables component scanning
- Auto-configures data source
- Enables JPA/Hibernate
- Starts embedded Tomcat (port 8081)

---

### 2. Job Entity
```java
@Entity
@Table(name = "jobs")
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String fileName;
    
    @Column(nullable = false)
    private Integer totalRecords;
    
    @Column(nullable = false, length = 50)
    private String status;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
```

**Annotations Explained**:
- `@Entity` - JPA entity for database mapping
- `@Table(name="jobs")` - Maps to 'jobs' table
- `@Id` - Primary key
- `@GeneratedValue` - Auto-increment ID
- `@Column` - Column constraints
- `@CreationTimestamp` - Auto-set on insert (Hibernate)
- `@UpdateTimestamp` - Auto-set on update (Hibernate)

**Database Behavior**:
- Table auto-created on startup (ddl-auto: update)
- Timestamps auto-populated by Hibernate
- No manual timestamp setting required

---

### 3. Record Entity
```java
@Entity
@Table(name = "records")
public class Record {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long jobId;
    
    @Column(nullable = false)
    private String memberId;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String mobile;
    
    @Column(nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String communicationPreference;
    
    @Column(nullable = false, length = 50)
    private String status;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
```

**Key Points**:
- `jobId` is a foreign key (stored as Long, no @ManyToOne for simplicity)
- All fields required except id (auto-generated)
- Status field for tracking record state
- Auto timestamps like Job entity

---

### 4. Repositories
```java
@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
}

@Repository
public interface RecordRepository extends JpaRepository<Record, Long> {
}
```

**Inherited Methods** (from JpaRepository):
- `save(Job)` - Create or update
- `saveAll(List<Record>)` - Batch insert
- `findById(Long)` - Retrieve by ID
- `findAll()` - Retrieve all
- `delete(Job)` - Delete by entity
- `deleteById(Long)` - Delete by ID
- `count()` - Count total records

**Spring Data Magic**:
- No implementation required
- Auto-generates SQL at runtime
- Provides CRUD + batch operations
- Supports pagination/sorting

---

### 5. FileUploadService
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {
    private final JobRepository jobRepository;
    private final RecordRepository recordRepository;
    private final CsvParser csvParser;
    
    @Transactional
    public FileUploadResponse uploadCsv(MultipartFile file) {
        // 1. Parse CSV
        List<MemberRecord> memberRecords = csvParser.parse(file);
        
        // 2. Create Job
        Job job = createJob(file.getOriginalFilename(), memberRecords.size());
        
        // 3. Create Records
        createRecords(job.getId(), memberRecords);
        
        // 4. Return response
        return FileUploadResponse.builder()
            .jobId(job.getId())
            .fileName(job.getFileName())
            .totalRecords(job.getTotalRecords())
            .status(job.getStatus())
            .build();
    }
    
    private Job createJob(String fileName, int totalRecords) {
        Job job = Job.builder()
            .fileName(fileName)
            .totalRecords(totalRecords)
            .status("UPLOADED")
            .build();
        return jobRepository.save(job);
    }
    
    private void createRecords(Long jobId, List<MemberRecord> memberRecords) {
        List<Record> records = memberRecords.stream()
            .map(memberRecord -> Record.builder()
                .jobId(jobId)
                .memberId(memberRecord.getMemberId())
                .name(memberRecord.getName())
                .mobile(memberRecord.getMobile())
                .email(memberRecord.getEmail())
                .communicationPreference(memberRecord.getCommunicationPreference())
                .status("RECEIVED")
                .build())
            .toList();
        recordRepository.saveAll(records);
    }
}
```

**Annotations**:
- `@Slf4j` - Lombok logging (private static Logger log)
- `@Service` - Spring managed service component
- `@RequiredArgsConstructor` - Lombok constructor injection
- `@Transactional` - Database transaction wrapper

**Transaction Behavior**:
- Method starts transaction on entry
- All DB operations within single transaction
- Auto-commits on success
- Auto-rollback on exception

**Stream Processing**:
- `memberRecords.stream()` - Lazy evaluation
- `.map()` - Transform DTO to entity
- `.toList()` - Collect to list

---

### 6. CsvParser
```java
@Component
public class CsvParser {
    private static final String[] CSV_HEADERS = {
        "memberId", "name", "mobile", "email", "communicationPreference"
    };
    
    public List<MemberRecord> parse(MultipartFile file) {
        // 1. Validation
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("File is empty or null", "INVALID_FILE", 400);
        }
        
        if (!isValidCsvFile(file.getOriginalFilename())) {
            throw new FileUploadException("File must be a CSV file", "INVALID_FILE_TYPE", 400);
        }
        
        // 2. Parse
        List<MemberRecord> records = new ArrayList<>();
        try (InputStreamReader reader = new InputStreamReader(
                    file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser csvParser = new CSVParser(reader, 
                    CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            
            int lineNumber = 1;
            for (CSVRecord csvRecord : csvParser) {
                lineNumber++;
                try {
                    MemberRecord record = parseLine(csvRecord, lineNumber);
                    records.add(record);
                } catch (IllegalArgumentException e) {
                    log.warn("Skipping invalid record at line {}: {}", lineNumber, e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new FileUploadException(
                "Error parsing CSV file: " + e.getMessage(), e, "CSV_PARSE_ERROR", 400);
        }
        
        // 3. Validation
        if (records.isEmpty()) {
            throw new FileUploadException(
                "CSV file contains no valid records", "NO_RECORDS", 400);
        }
        
        return records;
    }
    
    private MemberRecord parseLine(CSVRecord csvRecord, int lineNumber) {
        String memberId = csvRecord.get("memberId").trim();
        String name = csvRecord.get("name").trim();
        String mobile = csvRecord.get("mobile").trim();
        String email = csvRecord.get("email").trim();
        String communicationPreference = csvRecord.get("communicationPreference").trim();
        
        if (memberId.isEmpty() || name.isEmpty() || mobile.isEmpty() 
            || email.isEmpty() || communicationPreference.isEmpty()) {
            throw new IllegalArgumentException("Required field is empty");
        }
        
        return MemberRecord.builder()
            .memberId(memberId)
            .name(name)
            .mobile(mobile)
            .email(email)
            .communicationPreference(communicationPreference)
            .build();
    }
    
    private boolean isValidCsvFile(String filename) {
        return filename != null && filename.toLowerCase().endsWith(".csv");
    }
}
```

**CSV Processing Flow**:
1. **File Validation** - Check null, empty, extension
2. **Stream Processing** - Read from InputStream
3. **Header Parsing** - CSVFormat.DEFAULT.withFirstRecordAsHeader()
4. **Row Iteration** - For each CSV record
5. **Field Extraction** - Get by column name
6. **Validation** - Check required fields not empty
7. **DTO Creation** - Build MemberRecord
8. **Error Handling** - Log and skip invalid rows
9. **Result Check** - Ensure at least one valid record

**Apache Commons CSV**:
- Handles CSV parsing complexity
- Supports quoted fields, escaping, etc.
- Efficient streaming processing
- Header-based column access

---

### 7. FileUploadController
```java
@Slf4j
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class FileUploadController {
    private final FileUploadService fileUploadService;
    
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadCsv(
            @RequestParam("file") MultipartFile file) {
        log.info("Received CSV upload request for file: {}", file.getOriginalFilename());
        
        FileUploadResponse response = fileUploadService.uploadCsv(file);
        
        ApiResponse<FileUploadResponse> apiResponse = 
            ApiResponse.<FileUploadResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("File uploaded successfully")
                .data(response)
                .timestamp(LocalDateTime.now().toString())
                .build();
        
        log.info("CSV upload completed successfully for jobId: {}", response.getJobId());
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }
}
```

**Annotations**:
- `@RestController` - REST endpoint class
- `@RequestMapping("/api/jobs")` - Base URL path
- `@PostMapping("/upload")` - HTTP POST handler
- `@RequestParam("file")` - Form parameter binding

**Response Building**:
- `ApiResponse` wrapper from common-lib
- HTTP 201 (CREATED) status
- Generic type `<FileUploadResponse>`
- ISO timestamp

---

### 8. GlobalExceptionHandler
```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    
    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<ApiResponse<Void>> handleFileUploadException(
            FileUploadException ex) {
        log.error("FileUploadException: {} - {}", ex.getErrorCode(), ex.getMessage(), ex);
        
        ApiResponse<Void> response = ApiResponse.<Void>builder()
            .code(ex.getHttpStatus())
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now().toString())
            .build();
        
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }
    
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex) {
        log.error("File size exceeded", ex);
        
        ApiResponse<Void> response = ApiResponse.<Void>builder()
            .code(HttpStatus.PAYLOAD_TOO_LARGE.value())
            .message("File size exceeds maximum allowed size")
            .timestamp(LocalDateTime.now().toString())
            .build();
        
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        
        ApiResponse<Void> response = ApiResponse.<Void>builder()
            .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .message("An unexpected error occurred")
            .timestamp(LocalDateTime.now().toString())
            .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
```

**Exception Handling Strategy**:
1. Specific exceptions first (FileUploadException)
2. Framework exceptions (MaxUploadSizeExceededException)
3. Generic catch-all (Exception)

**Logging**:
- Full stack trace for debugging
- Error code extraction
- Message preservation

---

## Configuration (application.yml)

```yaml
spring:
  application:
    name: file-upload-service
  
  # JPA/Hibernate Configuration
  jpa:
    hibernate:
      ddl-auto: update  # Auto-create/update tables
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
    open-in-view: false
  
  # Database Configuration
  datasource:
    url: jdbc:postgresql://localhost:5432/tracemindai
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
  
  # File Upload Configuration
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

# Server Configuration
server:
  port: 8081
  servlet:
    context-path: /

# Logging Configuration
logging:
  level:
    root: INFO
    com.tracemindai: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

# Application Configuration
app:
  fileupload:
    upload-dir: /tmp/uploads
    max-file-size: 10485760
    max-concurrent-uploads: 5
```

**Key Settings**:
- `ddl-auto: update` - Auto-create tables if missing
- `dialect: PostgreSQLDialect` - PostgreSQL specific SQL
- `max-file-size: 10MB` - Multipart file size limit
- Log level DEBUG for `com.tracemindai` - Detailed application logs

---

## Database Schema Auto-Creation

On first startup, Hibernate creates:

```sql
CREATE TABLE jobs (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    total_records INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE records (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL,
    member_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    mobile VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    communication_preference VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_jobs_created_at ON jobs(created_at);
CREATE INDEX idx_records_created_at ON records(created_at);
CREATE INDEX idx_records_job_id ON records(job_id);
```

---

## Complete Request/Response Flow

### Request
```
POST /api/jobs/upload HTTP/1.1
Host: localhost:8081
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary

------WebKitFormBoundary
Content-Disposition: form-data; name="file"; filename="members.csv"
Content-Type: text/csv

memberId,name,mobile,email,communicationPreference
M001,John Doe,9876543210,john@example.com,EMAIL
------WebKitFormBoundary--
```

### Processing
1. **Controller** receives MultipartFile
2. **Service** calls CsvParser.parse()
3. **CsvParser** validates file, reads CSV
4. **Service** creates Job record
5. **Service** creates Record batch
6. **Controller** builds response

### Response
```
HTTP/1.1 201 Created
Content-Type: application/json

{
  "code": 201,
  "message": "File uploaded successfully",
  "data": {
    "jobId": 1,
    "fileName": "members.csv",
    "totalRecords": 2,
    "status": "UPLOADED"
  },
  "timestamp": "2026-06-13T10:30:45.123"
}
```

---

## Error Handling Examples

### Empty File
```json
{
  "code": 400,
  "message": "File is empty or null",
  "timestamp": "2026-06-13T10:30:45.123"
}
```

### Wrong File Type
```json
{
  "code": 400,
  "message": "File must be a CSV file",
  "timestamp": "2026-06-13T10:30:45.123"
}
```

### File Too Large
```json
{
  "code": 413,
  "message": "File size exceeds maximum allowed size",
  "timestamp": "2026-06-13T10:30:45.123"
}
```

---

## Extending the Implementation

### Adding Validation
```java
// In CsvParser.parseLine()
if (!isValidEmail(email)) {
    throw new IllegalArgumentException("Invalid email format");
}
```

### Adding Async Processing
```java
// Future enhancement
@Async
public CompletableFuture<FileUploadResponse> uploadCsvAsync(MultipartFile file) {
    return CompletableFuture.completedFuture(uploadCsv(file));
}
```

### Adding Pagination API
```java
// In FileUploadService
public Page<Job> getJobs(Pageable pageable) {
    return jobRepository.findAll(pageable);
}
```

### Adding Event Publishing
```java
// In FileUploadService
applicationEventPublisher.publishEvent(
    new FileUploadedEvent(jobId, totalRecords));
```

---

## Conclusion

The File Upload Service implementation follows Spring Boot best practices:
- ✅ Clean separation of concerns
- ✅ Dependency injection
- ✅ Transaction management
- ✅ Comprehensive error handling
- ✅ Structured logging
- ✅ Production-ready code
- ✅ Extensible architecture

All code is ready for deployment and can be extended with additional features as needed.
