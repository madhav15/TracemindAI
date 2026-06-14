# File Upload Service

A Spring Boot microservice for uploading and processing CSV files containing member records.

## Overview

The File Upload Service provides a REST API endpoint that accepts CSV files, parses them, and stores the data in PostgreSQL. Each uploaded file creates a Job record and multiple Record entries (one per CSV line).

## Features

- CSV file upload and parsing
- Automatic database table creation
- PostgreSQL integration
- Clean architecture with separation of concerns
- Global exception handling
- Transaction support
- Structured logging

## Prerequisites

- Java 21
- PostgreSQL 12+
- Maven 3.8+

## Setup

### 1. PostgreSQL Configuration

Create a PostgreSQL database:

```bash
createdb tracemindai
```

If using the default PostgreSQL user, ensure your credentials match the configuration in `application.yml`.

### 2. Build the Service

```bash
cd /Users/madhav/Projects/java/TracemindAI/file-upload-service
mvn clean install
```

### 3. Run the Service

```bash
mvn spring-boot:run
```

The service will start on `http://localhost:8081`

Database tables will be automatically created on startup:
- `jobs` - Stores file upload job information
- `records` - Stores individual member records from CSV files

## API Endpoint

### Upload CSV File

**Endpoint:** `POST /api/jobs/upload`

**Request:**
- Method: POST
- Content-Type: multipart/form-data
- Parameter: `file` (MultipartFile, CSV format)

**CSV Format:**

The CSV file must contain the following columns (with header row):
```
memberId,name,mobile,email,communicationPreference
```

Example CSV:
```csv
memberId,name,mobile,email,communicationPreference
M001,John Doe,9876543210,john@example.com,EMAIL
M002,Jane Smith,9876543211,jane@example.com,SMS
M003,Bob Johnson,9876543212,bob@example.com,BOTH
```

**Response:** 201 Created

```json
{
  "code": 201,
  "message": "File uploaded successfully",
  "data": {
    "jobId": 1,
    "fileName": "members.csv",
    "totalRecords": 3,
    "status": "UPLOADED"
  },
  "timestamp": "2026-06-13T10:30:45.123"
}
```

## Testing with cURL

### Upload a CSV file:

```bash
curl -X POST http://localhost:8081/api/jobs/upload \
  -F "file=@members.csv"
```

### Create a test CSV file:

```bash
cat > test.csv << 'EOF'
memberId,name,mobile,email,communicationPreference
M001,John Doe,9876543210,john@example.com,EMAIL
M002,Jane Smith,9876543211,jane@example.com,SMS
M003,Bob Johnson,9876543212,bob@example.com,PHONE
EOF
```

Then upload:
```bash
curl -X POST http://localhost:8081/api/jobs/upload \
  -F "file=@test.csv"
```

## Database Schema

### Jobs Table

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT |
| fileName | VARCHAR(255) | NOT NULL |
| totalRecords | INTEGER | NOT NULL |
| status | VARCHAR(50) | NOT NULL |
| createdAt | TIMESTAMP | NOT NULL, AUTO_GENERATED |
| updatedAt | TIMESTAMP | NOT NULL, AUTO_GENERATED |

### Records Table

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT |
| jobId | BIGINT | NOT NULL |
| memberId | VARCHAR(255) | NOT NULL |
| name | VARCHAR(255) | NOT NULL |
| mobile | VARCHAR(20) | NOT NULL |
| email | VARCHAR(255) | NOT NULL |
| communicationPreference | VARCHAR(100) | NOT NULL |
| status | VARCHAR(50) | NOT NULL |
| createdAt | TIMESTAMP | NOT NULL, AUTO_GENERATED |
| updatedAt | TIMESTAMP | NOT NULL, AUTO_GENERATED |

## Error Handling

The service provides structured error responses:

### 400 Bad Request - Empty File
```json
{
  "code": 400,
  "message": "File is empty or null",
  "timestamp": "2026-06-13T10:30:45.123"
}
```

### 400 Bad Request - Invalid File Type
```json
{
  "code": 400,
  "message": "File must be a CSV file",
  "timestamp": "2026-06-13T10:30:45.123"
}
```

### 400 Bad Request - No Valid Records
```json
{
  "code": 400,
  "message": "CSV file contains no valid records",
  "timestamp": "2026-06-13T10:30:45.123"
}
```

### 413 Payload Too Large
```json
{
  "code": 413,
  "message": "File size exceeds maximum allowed size",
  "timestamp": "2026-06-13T10:30:45.123"
}
```

### 500 Internal Server Error
```json
{
  "code": 500,
  "message": "An unexpected error occurred",
  "timestamp": "2026-06-13T10:30:45.123"
}
```

## Configuration

### Application Properties (application.yml)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/tracemindai
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: update
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

server:
  port: 8081

app:
  fileupload:
    upload-dir: /tmp/uploads
    max-file-size: 10485760
    max-concurrent-uploads: 5
```

## Architecture

### Package Structure

```
com.tracemindai.fileupload
├── controller/          # REST endpoints
├── service/            # Business logic
├── repository/         # Data access layer
├── entity/            # JPA entities
├── dto/               # Data transfer objects
├── exception/         # Custom exceptions and handlers
├── util/              # Utility classes (CSV parsing)
└── FileUploadServiceApplication.java
```

### Key Classes

- **FileUploadController** - REST endpoint handler
- **FileUploadService** - Business logic for CSV processing
- **CsvParser** - CSV file parsing utility
- **Job & Record Entities** - JPA domain models
- **JobRepository & RecordRepository** - Spring Data JPA repositories
- **GlobalExceptionHandler** - Centralized exception handling

## Workflow

1. **Request** - Client sends CSV file to `/api/jobs/upload`
2. **Validation** - CsvParser validates file type and content
3. **CSV Parsing** - Records are parsed from CSV format
4. **Job Creation** - A new Job record is created with status "UPLOADED"
5. **Record Creation** - Individual Record entries are created with status "RECEIVED"
6. **Response** - Returns job details with assigned jobId

## Logging

The service uses SLF4J with Logback for structured logging:

- **Root Level**: INFO
- **Application Level** (com.tracemindai): DEBUG

Logs include:
- CSV upload requests
- Record parsing details
- Database operations
- Error traces

## Dependencies

- Spring Boot 3.3.0
- Spring Data JPA
- PostgreSQL Driver
- Apache Commons CSV
- Lombok
- SLF4J/Logback

## Performance Notes

- **Database Transactions**: All operations within a single @Transactional context
- **Batch Inserts**: Records are inserted in batch for efficiency
- **CSV Parsing**: Streaming parser to handle large files
- **Connection Pooling**: HikariCP (default Spring Boot connection pool)

## Future Enhancements

- Async file processing with progress tracking
- File storage on disk
- Record validation against business rules
- Kafka integration for event publishing
- Pagination API to retrieve jobs and records
- Record search and filtering
- Bulk operations support

## Troubleshooting

### PostgreSQL Connection Error
- Verify PostgreSQL is running: `psql -U postgres -d tracemindai`
- Check credentials in `application.yml`
- Ensure database `tracemindai` exists

### CSV Parsing Errors
- Ensure CSV has proper header row with exact column names
- Check for special characters in CSV data
- Verify file encoding is UTF-8

### File Size Limit Exceeded
- Increase `spring.servlet.multipart.max-file-size` in application.yml
- Restart the application after changes

## License

Copyright 2026 TracemindAI. All rights reserved.
