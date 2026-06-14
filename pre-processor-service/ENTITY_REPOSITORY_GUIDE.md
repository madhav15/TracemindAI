# Pre-Processor Service - Entity and Repository Guide

## Overview

This guide documents the `PreProcessorTracking` JPA entity and `PreProcessorTrackingRepository` for the Pre-Processor Service.

## Database Table: pre_processor_tracking

The service tracks the preprocessing status of records from the File Upload Service.

### Column Mapping

| Java Field | Database Column | Type | Nullable | Constraints |
|------------|-----------------|------|----------|-------------|
| id | id | BIGINT | NO | PRIMARY KEY, AUTO_INCREMENT |
| jobId | job_id | VARCHAR(255) | NO | |
| recordId | record_id | VARCHAR(255) | NO | |
| memberId | member_id | VARCHAR(255) | NO | |
| status | status | VARCHAR(50) | NO | |
| processingType | processing_type | VARCHAR(100) | YES | |
| errorMessage | error_message | TEXT | YES | |
| createdAt | created_at | TIMESTAMP | NO | AUTO-GENERATED (immutable) |
| updatedAt | updated_at | TIMESTAMP | NO | AUTO-GENERATED |

## PreProcessorTracking Entity

### Class Definition

```java
@Entity
@Table(name = "pre_processor_tracking")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreProcessorTracking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "job_id", nullable = false)
    private String jobId;

    @Column(name = "record_id", nullable = false)
    private String recordId;

    @Column(name = "member_id", nullable = false)
    private String memberId;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "processing_type", length = 100)
    private String processingType;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
```

### Field Descriptions

#### id
- **Type**: Long
- **JPA**: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)`
- **Database**: BIGINT PRIMARY KEY AUTO_INCREMENT
- **Purpose**: Unique identifier for the tracking record
- **Auto-generated**: Yes

#### jobId
- **Type**: String
- **Database Column**: job_id (VARCHAR 255)
- **Nullable**: No
- **Purpose**: References the Job ID from file-upload-service
- **Example**: "JOB1234567890"
- **Usage**: Track which upload job this record belongs to

#### recordId
- **Type**: String
- **Database Column**: record_id (VARCHAR 255)
- **Nullable**: No
- **Unique**: Yes (implied from Kafka event structure)
- **Purpose**: Unique identifier for the record being processed
- **Example**: "REC0987654321"
- **Usage**: Correlate with RecordCreatedEvent from Kafka

#### memberId
- **Type**: String
- **Database Column**: member_id (VARCHAR 255)
- **Nullable**: No
- **Purpose**: Identifies the member/person associated with the record
- **Example**: "M001"
- **Usage**: Track which member's data is being processed

#### status
- **Type**: String
- **Database Column**: status (VARCHAR 50)
- **Nullable**: No
- **Length**: 50 characters
- **Purpose**: Current processing status of the record
- **Possible Values**: PENDING, PROCESSING, COMPLETED, FAILED, SKIPPED
- **Usage**: Query records by status for batch operations

#### processingType
- **Type**: String
- **Database Column**: processing_type (VARCHAR 100)
- **Nullable**: Yes
- **Length**: 100 characters
- **Purpose**: Type of preprocessing applied
- **Example Values**: "OCR", "DATA_VALIDATION", "TEXT_EXTRACTION", "FORMAT_CONVERSION"
- **Usage**: Track what kind of processing was performed

#### errorMessage
- **Type**: String
- **Database Column**: error_message (TEXT)
- **Nullable**: Yes
- **Constraint**: `columnDefinition = "TEXT"` for large text
- **Purpose**: Store error details if processing fails
- **Usage**: Debugging failed records

#### createdAt
- **Type**: LocalDateTime
- **Database Column**: created_at (TIMESTAMP)
- **Nullable**: No
- **Updatable**: No (immutable after creation)
- **JPA**: `@CreationTimestamp`
- **Auto-generated**: Yes (by Hibernate)
- **Timezone**: UTC (database default)
- **Purpose**: Track when the record was first processed
- **Usage**: Audit trail, filtering recent records

#### updatedAt
- **Type**: LocalDateTime
- **Database Column**: updated_at (TIMESTAMP)
- **Nullable**: No
- **JPA**: `@UpdateTimestamp`
- **Auto-updated**: Yes (by Hibernate on every update)
- **Timezone**: UTC (database default)
- **Purpose**: Track when the record was last updated
- **Usage**: Find stale records, status change history

## Column Name Mapping Annotations

Each field uses explicit `@Column` annotations following snake_case database naming convention:

```java
@Column(name = "job_id", nullable = false)
private String jobId;

@Column(name = "record_id", nullable = false)
private String recordId;

@Column(name = "member_id", nullable = false)
private String memberId;

@Column(name = "created_at", nullable = false, updatable = false)
private LocalDateTime createdAt;
```

This explicit mapping ensures:
- Clear visibility of database column names
- Maintenance of naming conventions (camelCase in Java, snake_case in DB)
- Flexibility to change column names without changing Java field names
- Better IDE autocomplete and refactoring support

## PreProcessorTrackingRepository

### Class Definition

```java
@Repository
public interface PreProcessorTrackingRepository 
        extends JpaRepository<PreProcessorTracking, Long> {
    Optional<PreProcessorTracking> findByRecordId(String recordId);
    
    List<PreProcessorTracking> findByJobId(String jobId);
    
    List<PreProcessorTracking> findByStatus(String status);
    
    List<PreProcessorTracking> findByJobIdAndStatus(String jobId, String status);
}
```

### Inherited Methods (from JpaRepository)

#### Create/Update

```java
// Save new or update existing
PreProcessorTracking save(PreProcessorTracking entity);

// Save multiple records
List<PreProcessorTracking> saveAll(Iterable<PreProcessorTracking> entities);
```

#### Read

```java
// Find by ID
Optional<PreProcessorTracking> findById(Long id);

// Find all records
List<PreProcessorTracking> findAll();

// Check if exists
boolean existsById(Long id);

// Get total count
long count();
```

#### Delete

```java
// Delete by ID
void deleteById(Long id);

// Delete by entity
void delete(PreProcessorTracking entity);

// Delete multiple
void deleteAll(Iterable<PreProcessorTracking> entities);

// Delete all
void deleteAll();
```

### Custom Query Methods

#### findByRecordId
```java
Optional<PreProcessorTracking> findByRecordId(String recordId);
```
- **Purpose**: Find tracking record by recordId
- **Returns**: Optional (record may not exist yet)
- **Usage**: Check if record has been processed
- **Example**:
  ```java
  Optional<PreProcessorTracking> tracking = 
      repository.findByRecordId("REC789");
  ```

#### findByJobId
```java
List<PreProcessorTracking> findByJobId(String jobId);
```
- **Purpose**: Find all tracking records for a specific job
- **Returns**: List (can be empty)
- **Usage**: Get all records in a job for batch operations
- **Example**:
  ```java
  List<PreProcessorTracking> jobRecords = 
      repository.findByJobId("JOB123");
  ```

#### findByStatus
```java
List<PreProcessorTracking> findByStatus(String status);
```
- **Purpose**: Find all records with a specific status
- **Returns**: List of matching records
- **Usage**: Find pending, failed, or completed records
- **Example**:
  ```java
  List<PreProcessorTracking> pending = 
      repository.findByStatus("PENDING");
  ```

#### findByJobIdAndStatus
```java
List<PreProcessorTracking> findByJobIdAndStatus(String jobId, String status);
```
- **Purpose**: Find records in a job with specific status
- **Returns**: List of matching records
- **Usage**: Get failed records from a job, completed records from a job
- **Example**:
  ```java
  List<PreProcessorTracking> failedInJob = 
      repository.findByJobIdAndStatus("JOB123", "FAILED");
  ```

## Usage Examples

### Create a Tracking Record

```java
@Service
public class PreProcessorService {
    @Autowired
    private PreProcessorTrackingRepository repository;
    
    public void trackRecordStart(String jobId, String recordId, String memberId) {
        PreProcessorTracking tracking = PreProcessorTracking.builder()
            .jobId(jobId)
            .recordId(recordId)
            .memberId(memberId)
            .status("PENDING")
            .processingType("OCR")
            .build();
        
        repository.save(tracking);
    }
}
```

### Update Status on Completion

```java
public void markCompleted(String recordId) {
    Optional<PreProcessorTracking> tracking = 
        repository.findByRecordId(recordId);
    
    tracking.ifPresent(t -> {
        t.setStatus("COMPLETED");
        repository.save(t);
    });
}
```

### Record Processing Failure

```java
public void recordError(String recordId, String errorMsg) {
    Optional<PreProcessorTracking> tracking = 
        repository.findByRecordId(recordId);
    
    tracking.ifPresent(t -> {
        t.setStatus("FAILED");
        t.setErrorMessage(errorMsg);
        repository.save(t);
    });
}
```

### Get Job Progress

```java
public JobProgress getProgress(String jobId) {
    List<PreProcessorTracking> allRecords = 
        repository.findByJobId(jobId);
    
    long completed = repository.findByJobIdAndStatus(jobId, "COMPLETED").size();
    long failed = repository.findByJobIdAndStatus(jobId, "FAILED").size();
    long pending = repository.findByJobIdAndStatus(jobId, "PENDING").size();
    
    return JobProgress.builder()
        .jobId(jobId)
        .total(allRecords.size())
        .completed(completed)
        .failed(failed)
        .pending(pending)
        .build();
}
```

### Batch Process Pending Records

```java
public void processPendingRecords() {
    List<PreProcessorTracking> pending = 
        repository.findByStatus("PENDING");
    
    for (PreProcessorTracking record : pending) {
        processRecord(record);
        record.setStatus("PROCESSING");
        repository.save(record);
    }
}
```

## Kafka Integration Point

The `PreProcessorTracking` entity will receive data from Kafka `RecordCreatedEvent`:

```
Kafka Topic: record-created
    ↓
RecordCreatedEvent
  {
    jobId: "JOB123",
    recordId: "REC456",
    memberId: "M001",
    communicationPreference: "EMAIL"
  }
    ↓
Kafka Listener (to be implemented)
    ↓
Create PreProcessorTracking
  {
    jobId: "JOB123",
    recordId: "REC456",
    memberId: "M001",
    status: "PENDING",
    processingType: null,
    errorMessage: null
  }
    ↓
Database: pre_processor_tracking
```

## Naming Conventions

### Java Field Naming (camelCase)
- `jobId`
- `recordId`
- `memberId`
- `processingType`
- `errorMessage`
- `createdAt`
- `updatedAt`

### Database Column Naming (snake_case)
- `job_id`
- `record_id`
- `member_id`
- `processing_type`
- `error_message`
- `created_at`
- `updated_at`

### Mapping via @Column
```java
@Column(name = "job_id")
private String jobId;
```

## Entity Lifecycle

### Creation
```java
// 1. Create entity
PreProcessorTracking tracking = new PreProcessorTracking();
tracking.setJobId("JOB123");
tracking.setRecordId("REC456");
tracking.setMemberId("M001");
tracking.setStatus("PENDING");

// 2. Save to database
repository.save(tracking);
// Automatically sets: id (generated), createdAt (now), updatedAt (now)
```

### Updates
```java
// 1. Fetch entity
Optional<PreProcessorTracking> tracking = 
    repository.findByRecordId("REC456");

// 2. Update fields
tracking.ifPresent(t -> {
    t.setStatus("PROCESSING");
    t.setProcessingType("OCR");
    repository.save(t);
    // Automatically updates: updatedAt (now)
    // createdAt remains unchanged
});
```

### Deletion
```java
// Delete by ID
repository.deleteById(id);

// Delete by entity
repository.delete(tracking);
```

## Validation and Constraints

### Database Constraints
- `job_id`: NOT NULL, indexed (for queries)
- `record_id`: NOT NULL, indexed (for queries)
- `member_id`: NOT NULL
- `status`: NOT NULL
- `created_at`: NOT NULL, immutable
- `updated_at`: NOT NULL

### Data Validation (future)
- Status values should be from an enum
- ProcessingType should be validated
- Error message length limits

## Timestamp Behavior

### createdAt
- Set automatically on INSERT
- Never updated
- Hibernate annotation: `@CreationTimestamp`
- JPA configuration: `updatable = false`

### updatedAt
- Set automatically on INSERT
- Updated on every UPDATE
- Hibernate annotation: `@UpdateTimestamp`
- Automatically managed by Hibernate

### Timezone
- Both timestamps stored in UTC
- Database: TIMESTAMP type
- Java: LocalDateTime (no timezone info, assumes UTC)

## Performance Considerations

### Indexes
Recommended database indexes:
```sql
CREATE INDEX idx_pre_processor_tracking_record_id 
    ON pre_processor_tracking(record_id);
    
CREATE INDEX idx_pre_processor_tracking_job_id 
    ON pre_processor_tracking(job_id);
    
CREATE INDEX idx_pre_processor_tracking_status 
    ON pre_processor_tracking(status);
    
CREATE INDEX idx_pre_processor_tracking_created_at 
    ON pre_processor_tracking(created_at);
```

### Query Performance
- `findByRecordId`: O(1) - indexed, single result
- `findByJobId`: O(n) - indexed, multiple results
- `findByStatus`: O(n) - indexed, multiple results
- `findByJobIdAndStatus`: O(n) - compound query

## Production Checklist

- [x] Entity created with proper annotations
- [x] All fields have explicit @Column mappings
- [x] camelCase Java fields match snake_case database columns
- [x] Repository interface created
- [x] Custom query methods defined
- [x] JPA configuration: `ddl-auto: validate` (no auto-migration)
- [x] Timestamps auto-managed by Hibernate
- [x] Ready for Kafka listener integration
- [ ] Business logic implementation (next phase)
- [ ] Kafka consumer configuration (next phase)

## Next Steps

1. ✅ Entity and Repository created
2. Create Kafka listener to consume `RecordCreatedEvent`
3. Implement preprocessing business logic
4. Update tracking records with results
5. Publish preprocessing completion events
6. Create queries API to expose tracking data

---

**Status**: ✅ Entity and Repository Complete - Ready for Business Logic Integration
