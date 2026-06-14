# File Upload Service - Testing Guide

## Prerequisites

1. **PostgreSQL Running**
   ```bash
   # Start PostgreSQL (macOS with Homebrew)
   brew services start postgresql

   # Or create database manually
   createdb tracemindai
   ```

2. **Service Running**
   ```bash
   cd /Users/madhav/Projects/java/TracemindAI/file-upload-service
   mvn spring-boot:run
   ```

   Expected output:
   ```
   Started FileUploadServiceApplication in X.XXX seconds
   ```

## Test Cases

### Test 1: Successful CSV Upload

**File:** `sample-members.csv`

```bash
curl -X POST http://localhost:8081/api/jobs/upload \
  -F "file=@sample-members.csv"
```

**Expected Response (201):**
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

**Verify in Database:**
```bash
psql -U postgres -d tracemindai

-- Check job was created
SELECT * FROM jobs;

-- Check records were created
SELECT * FROM records WHERE job_id = 1;

-- Verify counts match
SELECT job_id, COUNT(*) FROM records GROUP BY job_id;
```

---

### Test 2: Empty File

**Create empty file:**
```bash
touch empty.csv
```

**Upload:**
```bash
curl -X POST http://localhost:8081/api/jobs/upload \
  -F "file=@empty.csv"
```

**Expected Response (400):**
```json
{
  "code": 400,
  "message": "File is empty or null",
  "timestamp": "2026-06-13T10:30:45.123"
}
```

---

### Test 3: Invalid File Type

**Create text file:**
```bash
echo "memberId,name,mobile,email,communicationPreference" > invalid.txt
```

**Upload:**
```bash
curl -X POST http://localhost:8081/api/jobs/upload \
  -F "file=@invalid.txt"
```

**Expected Response (400):**
```json
{
  "code": 400,
  "message": "File must be a CSV file",
  "timestamp": "2026-06-13T10:30:45.123"
}
```

---

### Test 4: CSV with No Valid Records

**Create CSV with only headers:**
```bash
cat > headers_only.csv << 'EOF'
memberId,name,mobile,email,communicationPreference
EOF
```

**Upload:**
```bash
curl -X POST http://localhost:8081/api/jobs/upload \
  -F "file=@headers_only.csv"
```

**Expected Response (400):**
```json
{
  "code": 400,
  "message": "CSV file contains no valid records",
  "timestamp": "2026-06-13T10:30:45.123"
}
```

---

### Test 5: CSV with Missing Required Field

**Create CSV with missing email:**
```bash
cat > missing_field.csv << 'EOF'
memberId,name,mobile,email,communicationPreference
M001,John Doe,9876543210,,EMAIL
EOF
```

**Upload:**
```bash
curl -X POST http://localhost:8081/api/jobs/upload \
  -F "file=@missing_field.csv"
```

**Expected Response (400):**
```json
{
  "code": 400,
  "message": "CSV file contains no valid records",
  "timestamp": "2026-06-13T10:30:45.123"
}
```

The invalid record at line 2 is skipped, and since no valid records remain, an error is returned.

---

### Test 6: CSV with Mixed Valid and Invalid Records

**Create CSV with one invalid record:**
```bash
cat > mixed.csv << 'EOF'
memberId,name,mobile,email,communicationPreference
M001,John Doe,9876543210,john@example.com,EMAIL
M002,Jane Smith,9876543211,,SMS
M003,Bob Johnson,9876543212,bob@example.com,PHONE
EOF
```

**Upload:**
```bash
curl -X POST http://localhost:8081/api/jobs/upload \
  -F "file=@mixed.csv"
```

**Expected Response (201):**
```json
{
  "code": 201,
  "message": "File uploaded successfully",
  "data": {
    "jobId": 2,
    "fileName": "mixed.csv",
    "totalRecords": 2,
    "status": "UPLOADED"
  },
  "timestamp": "2026-06-13T10:30:45.123"
}
```

**Verify in Database:**
```bash
psql -U postgres -d tracemindai
SELECT * FROM records WHERE job_id = 2;
```

Only 2 records should exist (M001 and M003), M002 was skipped.

---

### Test 7: File Size Limit

**Create large CSV file (exceeds 10MB):**
```bash
# Create a file larger than max-file-size
python3 << 'EOF'
with open('large.csv', 'w') as f:
    f.write("memberId,name,mobile,email,communicationPreference\n")
    for i in range(1000000):
        f.write(f"M{i},Member {i},9876543210,member{i}@example.com,EMAIL\n")
EOF
```

**Upload:**
```bash
curl -X POST http://localhost:8081/api/jobs/upload \
  -F "file=@large.csv"
```

**Expected Response (413):**
```json
{
  "code": 413,
  "message": "File size exceeds maximum allowed size",
  "timestamp": "2026-06-13T10:30:45.123"
}
```

---

### Test 8: Multiple Consecutive Uploads

**Upload same file multiple times:**
```bash
for i in {1..3}; do
  curl -X POST http://localhost:8081/api/jobs/upload \
    -F "file=@sample-members.csv"
  echo "Upload $i completed\n"
done
```

**Verify in Database:**
```bash
psql -U postgres -d tracemindai
SELECT id, file_name, total_records FROM jobs;
-- Should show 3 separate job records with IDs 1, 2, 3

SELECT job_id, COUNT(*) as record_count FROM records GROUP BY job_id;
-- Should show each job has 10 records
```

---

### Test 9: Data Integrity

**Upload sample file and verify field preservation:**

```bash
curl -X POST http://localhost:8081/api/jobs/upload \
  -F "file=@sample-members.csv"
```

**Verify data in database:**
```bash
psql -U postgres -d tracemindai

SELECT * FROM records WHERE member_id = 'M005';
```

**Expected Output:**
```
id | job_id | member_id | name             | mobile     | email                       | communication_preference | status   | created_at           | updated_at
3  | 1      | M005      | Charlie Brown    | 9876543214 | charlie.brown@example.com   | BOTH                     | RECEIVED | 2026-06-13 10:30:45  | 2026-06-13 10:30:45
```

---

### Test 10: Concurrent Uploads

**Submit multiple uploads simultaneously:**

```bash
# Terminal 1
curl -X POST http://localhost:8081/api/jobs/upload -F "file=@sample-members.csv" &

# Terminal 2
curl -X POST http://localhost:8081/api/jobs/upload -F "file=@sample-members.csv" &

# Terminal 3
curl -X POST http://localhost:8081/api/jobs/upload -F "file=@sample-members.csv" &

# Wait for completion
wait
```

**Verify no data loss:**
```bash
psql -U postgres -d tracemindai
SELECT COUNT(*) FROM jobs;      -- Should be 3
SELECT COUNT(*) FROM records;   -- Should be 30 (3 jobs * 10 records each)
```

---

## Database Queries

### View All Jobs
```sql
SELECT id, file_name, total_records, status, created_at, updated_at 
FROM jobs 
ORDER BY created_at DESC;
```

### View All Records for a Job
```sql
SELECT * FROM records 
WHERE job_id = 1 
ORDER BY created_at;
```

### Count Records by Status
```sql
SELECT status, COUNT(*) as count 
FROM records 
GROUP BY status;
```

### Find Records by Member Email
```sql
SELECT * FROM records 
WHERE email LIKE '%@example.com' 
LIMIT 10;
```

### View Recent Uploads
```sql
SELECT id, file_name, total_records, status, created_at 
FROM jobs 
WHERE created_at >= NOW() - INTERVAL '1 hour' 
ORDER BY created_at DESC;
```

---

## Logs Analysis

Monitor application logs during testing:

```bash
# Terminal 2 (while running tests in Terminal 1)
tail -f /var/log/tracemindai/file-upload-service.log

# Or check Spring Boot logs in console
```

**Key Log Lines to Look For:**
- `Received CSV upload request for file: ...`
- `Successfully parsed X records from CSV file`
- `Created job with id: ... for X records`
- `Created X record entries in database`
- `CSV upload completed successfully for jobId: ...`

---

## Testing Checklist

- [x] Valid CSV upload succeeds
- [x] Empty file rejected
- [x] Invalid file type rejected
- [x] Missing required fields handled
- [x] File size limit enforced
- [x] Job record created with correct status
- [x] Record entries created with correct status
- [x] Database timestamps automatically populated
- [x] Concurrent uploads work correctly
- [x] Error responses properly formatted
- [x] Data integrity maintained

---

## Cleanup

After testing, you may want to clear the database:

```bash
psql -U postgres -d tracemindai

DELETE FROM records;
DELETE FROM jobs;

-- Reset auto-increment sequences
ALTER SEQUENCE records_id_seq RESTART WITH 1;
ALTER SEQUENCE jobs_id_seq RESTART WITH 1;
```

Or drop and recreate the entire database:

```bash
dropdb tracemindai
createdb tracemindai
# Restart application to recreate tables
```

---

## Troubleshooting

### "psql: connection refused"
- PostgreSQL not running: `brew services start postgresql`
- Or verify credentials in application.yml

### "relation \"jobs\" does not exist"
- Database tables not created: Restart application
- Or manually create tables (see schema in README)

### "File is empty or null"
- Ensure CSV file has content and headers

### "CSV file contains no valid records"
- Check CSV format matches required columns
- Verify no required fields are empty

### "Bad Request" without details
- Check application logs for detailed error message
- Verify request format: `curl -F "file=@filename.csv"`
