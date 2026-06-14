# Quick Start Guide - File Upload Service

## 1. Prerequisites
```bash
# Check Java 21 is installed
java -version

# Check Maven is installed
mvn -version

# Ensure PostgreSQL is running
psql --version
```

## 2. Create Database
```bash
# Create PostgreSQL database
createdb tracemindai

# Verify database was created
psql -l | grep tracemindai
```

## 3. Build Service
```bash
cd /Users/madhav/Projects/java/TracemindAI/file-upload-service
mvn clean install -DskipTests
```

Expected output: `BUILD SUCCESS`

## 4. Run Service
```bash
# Terminal 1: Start the service
mvn spring-boot:run
```

Wait for message: `Started FileUploadServiceApplication in X.XXX seconds`

## 5. Test Upload
```bash
# Terminal 2: Upload sample CSV
curl -X POST http://localhost:8081/api/jobs/upload \
  -F "file=@sample-members.csv"
```

Expected response (201 Created):
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
  "timestamp": "2026-06-13T..."
}
```

## 6. Verify Database
```bash
# Terminal 3: Connect to database
psql -U postgres -d tracemindai

# Check jobs table
SELECT * FROM jobs;

# Check records table
SELECT * FROM records LIMIT 5;
```

## Files Structure

```
file-upload-service/
├── pom.xml                      # Maven configuration
├── README.md                    # Full documentation
├── TESTING.md                   # Test cases and examples
├── ARCHITECTURE.md              # Architecture details
├── IMPLEMENTATION_GUIDE.md      # Implementation details
├── QUICK_START.md              # This file
├── sample-members.csv          # Sample test data
└── src/main/
    ├── java/com/tracemindai/fileupload/
    │   ├── FileUploadServiceApplication.java    # App entry point
    │   ├── controller/
    │   │   └── FileUploadController.java         # REST endpoint
    │   ├── service/
    │   │   └── FileUploadService.java            # Business logic
    │   ├── repository/
    │   │   ├── JobRepository.java                # Job persistence
    │   │   └── RecordRepository.java             # Record persistence
    │   ├── entity/
    │   │   ├── Job.java                          # Job entity
    │   │   └── Record.java                       # Record entity
    │   ├── dto/
    │   │   ├── FileUploadRequest.java
    │   │   ├── FileUploadResponse.java
    │   │   └── MemberRecord.java
    │   ├── exception/
    │   │   ├── FileUploadException.java
    │   │   └── GlobalExceptionHandler.java
    │   ├── config/
    │   │   └── FileUploadConfig.java
    │   └── util/
    │       ├── CsvParser.java                    # CSV parsing
    │       └── FileValidationUtil.java
    └── resources/
        └── application.yml                       # Configuration
```

## CSV Format

Required columns (case-sensitive):
```
memberId,name,mobile,email,communicationPreference
```

Example:
```csv
memberId,name,mobile,email,communicationPreference
M001,John Doe,9876543210,john@example.com,EMAIL
M002,Jane Smith,9876543211,jane@example.com,SMS
```

## API Endpoint

**POST** `/api/jobs/upload`

Request:
- Method: POST
- Content-Type: multipart/form-data
- Parameter: file (CSV file)

Response (201):
```json
{
  "code": 201,
  "message": "File uploaded successfully",
  "data": {
    "jobId": <number>,
    "fileName": "<original filename>",
    "totalRecords": <count>,
    "status": "UPLOADED"
  },
  "timestamp": "<ISO timestamp>"
}
```

## Troubleshooting

### PostgreSQL connection refused
```bash
# Start PostgreSQL
brew services start postgresql

# Or manually start
pg_ctl -D /usr/local/var/postgres start
```

### Database doesn't exist
```bash
createdb tracemindai
```

### Port 8081 already in use
```bash
# Change port in application.yml
server:
  port: 8082
```

### Build fails
```bash
# Clean cache and rebuild
mvn clean install -U
```

## Documentation

- **README.md** - Complete API documentation and configuration
- **TESTING.md** - Comprehensive test cases and verification
- **ARCHITECTURE.md** - System design and component details
- **IMPLEMENTATION_GUIDE.md** - Detailed implementation walkthrough

## Next Steps

1. ✅ Service is running on http://localhost:8081
2. ✅ CSV uploads work with sample-members.csv
3. ✅ Data is persisted to PostgreSQL
4. 📚 Read README.md for full API documentation
5. 🧪 Follow TESTING.md for all test scenarios
6. 🏗️ See ARCHITECTURE.md for design details

## Need Help?

1. Check application logs (terminal 1 output)
2. Review TESTING.md for common issues
3. Verify database connection with psql
4. Check application.yml configuration

---

**Status**: ✅ Ready for Production Use
