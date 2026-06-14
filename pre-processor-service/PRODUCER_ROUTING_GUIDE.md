# Pre-Processor Service - Producer & Routing Guide

## Overview

This guide documents the Kafka producer implementation and routing logic for the Pre-Processor Service.

## Architecture

```
RecordCreatedEvent (Kafka)
        ↓
PreProcessorService
        ↓
Route by Communication Preference
        ├─→ "E" (Email)
        │   ├─ Build EmailRequestEvent
        │   ├─ Publish to topic: email-request
        │   └─ Update tracking: status=ROUTED, processingType=EMAIL
        │
        ├─→ "P" (Print)
        │   ├─ Build PrintRequestEvent
        │   ├─ Publish to topic: print-request
        │   └─ Update tracking: status=ROUTED, processingType=PRINT
        │
        └─→ Unknown
            ├─ Update tracking: status=FAILED
            └─ Log warning
```

## Kafka Topics

### Email Request Topic
- **Name**: `email-request`
- **Message Type**: `EmailRequestEvent` (JSON)
- **Key**: `recordId` (String)
- **Consumers**: Email Service (future)

### Print Request Topic
- **Name**: `print-request`
- **Message Type**: `PrintRequestEvent` (JSON)
- **Key**: `recordId` (String)
- **Consumers**: Print Service (future)

## RequestEventProducer

### Class Definition

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class RequestEventProducer {
    private final KafkaTemplate<String, EmailRequestEvent> emailKafkaTemplate;
    private final KafkaTemplate<String, PrintRequestEvent> printKafkaTemplate;

    public void publishEmailRequest(EmailRequestEvent event)
    public void publishPrintRequest(PrintRequestEvent event)
}
```

### Method: publishEmailRequest

#### Signature
```java
public void publishEmailRequest(EmailRequestEvent event)
```

#### Parameters
```java
EmailRequestEvent {
    jobId: String,           // Job identifier
    recordId: String,        // Record identifier
    memberId: String,        // Member identifier
    correlationId: String,   // Tracing ID (= jobId)
    traceId: String         // Tracing ID (= recordId)
}
```

#### Behavior
1. **Publish**: Send event to `email-request` topic
2. **Key**: Use `recordId` for partitioning
3. **Log**: Debug message on success
4. **Error**: Throw `KafkaPublishException` on failure

#### Logging
```
DEBUG: Published EmailRequestEvent for recordId: REC123 to topic: email-request
ERROR: Failed to publish EmailRequestEvent for recordId: REC123
```

### Method: publishPrintRequest

#### Signature
```java
public void publishPrintRequest(PrintRequestEvent event)
```

#### Parameters
```java
PrintRequestEvent {
    jobId: String,           // Job identifier
    recordId: String,        // Record identifier
    memberId: String,        // Member identifier
    correlationId: String,   // Tracing ID (= jobId)
    traceId: String         // Tracing ID (= recordId)
}
```

#### Behavior
1. **Publish**: Send event to `print-request` topic
2. **Key**: Use `recordId` for partitioning
3. **Log**: Debug message on success
4. **Error**: Throw `KafkaPublishException` on failure

#### Logging
```
DEBUG: Published PrintRequestEvent for recordId: REC123 to topic: print-request
ERROR: Failed to publish PrintRequestEvent for recordId: REC123
```

## KafkaProducerConfig

### Class Definition

```java
@Configuration
@RequiredArgsConstructor
public class KafkaProducerConfig {
    @Bean
    public ProducerFactory<String, EmailRequestEvent> emailProducerFactory()
    
    @Bean
    public ProducerFactory<String, PrintRequestEvent> printProducerFactory()
    
    @Bean
    public KafkaTemplate<String, EmailRequestEvent> emailKafkaTemplate()
    
    @Bean
    public KafkaTemplate<String, PrintRequestEvent> printKafkaTemplate()
}
```

### Configuration Details

**Producer Configuration:**
- Bootstrap servers: localhost:29092 (from application.yml)
- Key serializer: StringSerializer
- Value serializer: JsonSerializer
- Acks: all (waits for all in-sync replicas)
- Retries: 0 (no automatic retries)

**Beans:**
- `emailProducerFactory`: Creates EmailRequestEvent producers
- `printProducerFactory`: Creates PrintRequestEvent producers
- `emailKafkaTemplate`: Template for publishing EmailRequestEvent
- `printKafkaTemplate`: Template for publishing PrintRequestEvent

## PreProcessorService Routing Logic

### Updated processRecord Method

```java
@Transactional
public void processRecord(RecordCreatedEvent event) {
    // 1. Create initial tracking record with status=PROCESSING
    PreProcessorTracking tracking = PreProcessorTracking.builder()
        .jobId(event.getJobId())
        .recordId(event.getRecordId())
        .memberId(event.getMemberId())
        .status("PROCESSING")
        .processingType(event.getCommunicationPreference())
        .build();
    
    repository.save(tracking);
    
    // 2. Route by preference (publishes events and updates tracking)
    routeByPreference(event);
}
```

### routeByPreference Method

```java
private void routeByPreference(RecordCreatedEvent event) {
    if (PREF_EMAIL.equals(event.getCommunicationPreference())) {
        // Email path
        publishEmailRequest(event);
        updateTrackingAsRouted(event.getRecordId(), TYPE_EMAIL);
        log.info("Routed record {} to EMAIL pipeline", event.getRecordId());
    } else if (PREF_PRINT.equals(event.getCommunicationPreference())) {
        // Print path
        publishPrintRequest(event);
        updateTrackingAsRouted(event.getRecordId(), TYPE_PRINT);
        log.info("Routed record {} to PRINT pipeline", event.getRecordId());
    } else {
        // Unknown preference path
        updateTrackingAsFailed(event.getRecordId());
        log.warn("Unknown communication preference: {} for record: {}",
            event.getCommunicationPreference(), event.getRecordId());
    }
}
```

### Email Request Path

**Step 1: Build EmailRequestEvent**
```java
private void publishEmailRequest(RecordCreatedEvent event) {
    EmailRequestEvent emailRequest = EmailRequestEvent.builder()
        .jobId(event.getJobId())
        .recordId(event.getRecordId())
        .memberId(event.getMemberId())
        .correlationId(event.getCorrelationId())
        .traceId(event.getTraceId())
        .build();
    
    eventProducer.publishEmailRequest(emailRequest);
}
```

**Step 2: Update Tracking**
```java
private void updateTrackingAsRouted(String recordId, String processingType) {
    PreProcessorTracking tracking = repository.findByRecordId(recordId).orElseThrow();
    tracking.setStatus("ROUTED");
    tracking.setProcessingType("EMAIL");
    repository.save(tracking);
}
```

**Result:**
```
Kafka Topic: email-request
  Message: {
    jobId: "JOB1234567890",
    recordId: "REC0987654321",
    memberId: "M001",
    correlationId: "JOB1234567890",
    traceId: "REC0987654321"
  }

Database: pre_processor_tracking
  status: "ROUTED"
  processingType: "EMAIL"
```

### Print Request Path

**Step 1: Build PrintRequestEvent**
```java
private void publishPrintRequest(RecordCreatedEvent event) {
    PrintRequestEvent printRequest = PrintRequestEvent.builder()
        .jobId(event.getJobId())
        .recordId(event.getRecordId())
        .memberId(event.getMemberId())
        .correlationId(event.getCorrelationId())
        .traceId(event.getTraceId())
        .build();
    
    eventProducer.publishPrintRequest(printRequest);
}
```

**Step 2: Update Tracking**
```java
private void updateTrackingAsRouted(String recordId, String processingType) {
    PreProcessorTracking tracking = repository.findByRecordId(recordId).orElseThrow();
    tracking.setStatus("ROUTED");
    tracking.setProcessingType("PRINT");
    repository.save(tracking);
}
```

**Result:**
```
Kafka Topic: print-request
  Message: {
    jobId: "JOB1234567890",
    recordId: "REC0987654321",
    memberId: "M001",
    correlationId: "JOB1234567890",
    traceId: "REC0987654321"
  }

Database: pre_processor_tracking
  status: "ROUTED"
  processingType: "PRINT"
```

### Unknown Preference Path

**Step: Update Tracking as Failed**
```java
private void updateTrackingAsFailed(String recordId) {
    PreProcessorTracking tracking = repository.findByRecordId(recordId).orElseThrow();
    tracking.setStatus("FAILED");
    repository.save(tracking);
}
```

**Result:**
```
No Kafka event published

Database: pre_processor_tracking
  status: "FAILED"
  processingType: null
```

## Complete Data Flow

### Input: RecordCreatedEvent
```json
{
  "event_id": "550e8400-e29b-41d4-a716-446655440000",
  "event_type": "RecordCreatedEvent",
  "created_at": "2026-06-14T10:30:45.123456",
  "source": "file-upload-service",
  "job_id": "JOB1234567890",
  "record_id": "REC0987654321",
  "member_id": "M001",
  "communication_preference": "E",
  "correlation_id": "JOB1234567890",
  "trace_id": "REC0987654321"
}
```

### Processing Steps

1. **Receive**: RecordCreatedEventListener receives event
2. **Create**: Save initial PreProcessorTracking with status=PROCESSING
3. **Route**: Check communication preference
4. **For Email**:
   - Build EmailRequestEvent
   - Publish to email-request topic
   - Update tracking: status=ROUTED, processingType=EMAIL
   - Log: INFO "Routed record REC... to EMAIL pipeline"
5. **For Print**:
   - Build PrintRequestEvent
   - Publish to print-request topic
   - Update tracking: status=ROUTED, processingType=PRINT
   - Log: INFO "Routed record REC... to PRINT pipeline"
6. **For Unknown**:
   - Update tracking: status=FAILED
   - Log: WARN "Unknown communication preference..."

### Output Examples

**Email Scenario:**
```
Database: pre_processor_tracking
  id: 1
  job_id: JOB1234567890
  record_id: REC0987654321
  member_id: M001
  status: ROUTED
  processing_type: EMAIL
  created_at: 2026-06-14 10:30:45
  updated_at: 2026-06-14 10:30:45

Kafka Topic: email-request
  Key: REC0987654321
  Value: {
    jobId: "JOB1234567890",
    recordId: "REC0987654321",
    memberId: "M001",
    correlationId: "JOB1234567890",
    traceId: "REC0987654321"
  }
```

**Print Scenario:**
```
Database: pre_processor_tracking
  id: 2
  job_id: JOB1234567890
  record_id: REC1111111111
  member_id: M002
  status: ROUTED
  processing_type: PRINT
  created_at: 2026-06-14 10:30:46
  updated_at: 2026-06-14 10:30:46

Kafka Topic: print-request
  Key: REC1111111111
  Value: {
    jobId: "JOB1234567890",
    recordId: "REC1111111111",
    memberId: "M002",
    correlationId: "JOB1234567890",
    traceId: "REC1111111111"
  }
```

**Unknown Scenario:**
```
Database: pre_processor_tracking
  id: 3
  job_id: JOB1234567890
  record_id: REC2222222222
  member_id: M003
  status: FAILED
  processing_type: null
  created_at: 2026-06-14 10:30:47
  updated_at: 2026-06-14 10:30:47

No Kafka event published
```

## Logging Output

### Email Route
```
DEBUG: Processing record: REC0987654321 from job: JOB1234567890
DEBUG: Saved tracking record for recordId: REC0987654321
DEBUG: Published EmailRequestEvent for recordId: REC0987654321 to topic: email-request
DEBUG: Updated tracking record for recordId: REC0987654321 with status: ROUTED and processingType: EMAIL
INFO:  Routed record REC0987654321 to EMAIL pipeline
```

### Print Route
```
DEBUG: Processing record: REC1111111111 from job: JOB1234567890
DEBUG: Saved tracking record for recordId: REC1111111111
DEBUG: Published PrintRequestEvent for recordId: REC1111111111 to topic: print-request
DEBUG: Updated tracking record for recordId: REC1111111111 with status: ROUTED and processingType: PRINT
INFO:  Routed record REC1111111111 to PRINT pipeline
```

### Unknown Route
```
DEBUG: Processing record: REC2222222222 from job: JOB1234567890
DEBUG: Saved tracking record for recordId: REC2222222222
DEBUG: Updated tracking record for recordId: REC2222222222 with status: FAILED
WARN:  Unknown communication preference: X for record: REC2222222222
```

## Constants

```java
private static final String STATUS_PROCESSING = "PROCESSING";
private static final String STATUS_ROUTED = "ROUTED";
private static final String STATUS_FAILED = "FAILED";
private static final String PREF_EMAIL = "E";
private static final String PREF_PRINT = "P";
private static final String TYPE_EMAIL = "EMAIL";
private static final String TYPE_PRINT = "PRINT";
```

## Transaction Management

### @Transactional Behavior
- Single transaction encompasses:
  1. Save initial tracking record
  2. Find tracking record for update
  3. Publish Kafka event
  4. Update tracking record with status/processingType

- **On Success**: Auto-commit all changes
- **On Exception**: Auto-rollback all database changes
- **Kafka**: Once published, not rolled back even if later DB update fails

## Error Handling

### Database Errors
- PreProcessorTracking not found → IllegalStateException
- Database save fails → Auto-rollback

### Kafka Publishing Errors
- KafkaPublishException thrown
- Logged as ERROR
- Propagated to caller
- Transaction rolls back

### Unknown Communication Preference
- Tracked as "FAILED" status
- No exception thrown
- No Kafka event published
- Logged as WARNING

## File Structure

```
pre-processor-service/
├── src/main/
│   ├── java/com/tracemindai/preprocessor/
│   │   ├── service/
│   │   │   └── PreProcessorService.java (UPDATED)
│   │   ├── kafka/
│   │   │   ├── RequestEventProducer.java (NEW)
│   │   │   ├── KafkaProducerConfig.java (NEW)
│   │   │   ├── RecordCreatedEventListener.java (existing)
│   │   │   └── KafkaConsumerConfig.java (existing)
│   │   └── entity/
│   │       └── PreProcessorTracking.java (existing)
│   └── resources/
│       └── application.yml (existing, includes producer config)
├── ENTITY_REPOSITORY_GUIDE.md (existing)
├── SERVICE_LISTENER_GUIDE.md (existing)
└── PRODUCER_ROUTING_GUIDE.md (NEW)
```

## Configuration

### application.yml
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:29092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 0
```

## Constraints Met

✅ Create Kafka producer component
✅ Configure email-request topic
✅ Configure print-request topic
✅ Build EmailRequestEvent with all required fields
✅ Build PrintRequestEvent with all required fields
✅ Publish to correct topics
✅ Update tracking: status = ROUTED, processingType = EMAIL/PRINT
✅ Handle unknown preference: status = FAILED, no publish
✅ No new services created
✅ No REST APIs
✅ No other module modifications
✅ Minimal, production-quality code

## Production Checklist

- [x] Kafka producer created
- [x] Event builder methods implemented
- [x] Tracking update methods implemented
- [x] Error handling in place
- [x] Comprehensive logging
- [x] Configuration complete
- [x] Transaction management
- [x] Documentation complete

---

**Status**: ✅ Producer & Routing Complete - Ready for Email/Print Services
