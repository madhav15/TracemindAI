# Pre-Processor Service - Service & Listener Guide

## Overview

This guide documents the `PreProcessorService` and Kafka listener implementation for the Pre-Processor Service.

## Architecture

```
File Upload Service (8081)
        ↓
   CSV Records
        ↓
Kafka Topic: record-created
        ↓
PreProcessorService (8082)
  ↓
  RecordCreatedEventListener
    ↓
    PreProcessorService.processRecord(event)
      ↓
      Create PreProcessorTracking
      ↓
      Save to Database
      ↓
      Route by Communication Preference
        ↓
        Log Routing Decision
```

## PreProcessorService

### Class Definition

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class PreProcessorService {
    private final PreProcessorTrackingRepository repository;

    @Transactional
    public void processRecord(RecordCreatedEvent event) {
        // 1. Create tracking entity
        // 2. Save to database
        // 3. Route by communication preference
    }
}
```

### Method: processRecord

#### Signature
```java
@Transactional
public void processRecord(RecordCreatedEvent event)
```

#### Parameters
- `event`: `RecordCreatedEvent` from Kafka containing:
  - `jobId`: Upload job identifier
  - `recordId`: Unique record identifier
  - `memberId`: Member/person identifier
  - `communicationPreference`: "E" (Email), "P" (Print), or other
  - `correlationId`: Tracing ID (= jobId)
  - `traceId`: Tracing ID (= recordId)

#### Workflow

**Step 1: Create PreProcessorTracking Entity**
```java
PreProcessorTracking tracking = PreProcessorTracking.builder()
    .jobId(event.getJobId())              // e.g., "JOB1234567890"
    .recordId(event.getRecordId())        // e.g., "REC0987654321"
    .memberId(event.getMemberId())        // e.g., "M001"
    .status(STATUS_PROCESSING)            // "PROCESSING"
    .build();
```

**Step 2: Save to Database**
```java
repository.save(tracking);
// Automatically sets:
// - id: auto-generated
// - createdAt: current timestamp
// - updatedAt: current timestamp
```

**Step 3: Route by Communication Preference**
```
if communicationPreference == "E":
    log.info("Routing record {} to EMAIL pipeline", recordId)
    
else if communicationPreference == "P":
    log.info("Routing record {} to PRINT pipeline", recordId)
    
else:
    log.warn("Unknown communication preference: {} for record: {}", 
             communicationPreference, recordId)
```

#### Constants
```java
private static final String STATUS_PROCESSING = "PROCESSING";
private static final String PREF_EMAIL = "E";
private static final String PREF_PRINT = "P";
```

#### Transactional Behavior
- Marked with `@Transactional`
- All database operations in single transaction
- Auto-commit on success
- Auto-rollback on exception

#### Logging
```
DEBUG: Processing record: REC123 from job: JOB456
DEBUG: Saved tracking record for recordId: REC123
INFO:  Routing record REC123 to EMAIL pipeline
WARN:  Unknown communication preference: X for record: REC123
```

#### Exceptions
- Database exceptions propagated to caller
- Logged automatically by Spring

### Usage Example

```java
// In Kafka listener
RecordCreatedEvent event = RecordCreatedEvent.builder()
    .jobId("JOB1234567890")
    .recordId("REC0987654321")
    .memberId("M001")
    .communicationPreference("E")
    .build();

preProcessorService.processRecord(event);
```

## RecordCreatedEventListener

### Class Definition

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class RecordCreatedEventListener {
    private final PreProcessorService preProcessorService;

    @KafkaListener(
        topics = "record-created",
        groupId = "pre-processor-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onRecordCreated(RecordCreatedEvent event) {
        preProcessorService.processRecord(event);
    }
}
```

### Kafka Listener Configuration

#### Topic
- **Name**: `record-created`
- **Source**: File Upload Service
- **Message Type**: `RecordCreatedEvent` (JSON)
- **Key**: `recordId` (String)

#### Consumer Group
- **Group ID**: `pre-processor-group`
- **Purpose**: Track consumer offset
- **Instances**: Can scale horizontally

#### Container Factory
- **Name**: `kafkaListenerContainerFactory`
- **Configured in**: `KafkaConsumerConfig`
- **Type**: `ConcurrentKafkaListenerContainerFactory<String, RecordCreatedEvent>`

### Method: onRecordCreated

#### Signature
```java
@KafkaListener(
    topics = "record-created",
    groupId = "pre-processor-group",
    containerFactory = "kafkaListenerContainerFactory"
)
public void onRecordCreated(RecordCreatedEvent event)
```

#### Parameters
- `event`: `RecordCreatedEvent` auto-deserialized from Kafka JSON

#### Workflow
1. **Receive**: Kafka message auto-deserialized to `RecordCreatedEvent`
2. **Log**: Debug message with recordId and jobId
3. **Process**: Call `preProcessorService.processRecord(event)`
4. **Handle**: Any exceptions logged and handled by Spring

#### Logging
```
DEBUG: Received RecordCreatedEvent for recordId: REC123 from jobId: JOB456
```

### Listener Characteristics
- **Thin**: Only receives event and delegates to service
- **Stateless**: No state between messages
- **Scalable**: Multiple instances in same consumer group
- **Reliable**: Kafka handles offset management

## KafkaConsumerConfig

### Class Definition

```java
@Configuration
@EnableKafka
@RequiredArgsConstructor
public class KafkaConsumerConfig {
    private final KafkaProperties kafkaProperties;

    @Bean
    public ConsumerFactory<String, RecordCreatedEvent> consumerFactory() { }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, RecordCreatedEvent>
            kafkaListenerContainerFactory() { }
}
```

### Configuration Details

#### ConsumerFactory
```java
@Bean
public ConsumerFactory<String, RecordCreatedEvent> consumerFactory() {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
    configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "pre-processor-group");
    configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
        StringDeserializer.class);
    configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
        JsonDeserializer.class);
    configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE,
        RecordCreatedEvent.class.getName());
    configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
    configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

    return new DefaultKafkaConsumerFactory<>(configProps);
}
```

**Configuration Options:**
- `BOOTSTRAP_SERVERS_CONFIG`: Kafka broker address
- `GROUP_ID_CONFIG`: Consumer group (from application.yml)
- `KEY_DESERIALIZER_CLASS_CONFIG`: String deserializer for key
- `VALUE_DESERIALIZER_CLASS_CONFIG`: JSON deserializer for value
- `VALUE_DEFAULT_TYPE`: Default type for JSON deserialization
- `TRUSTED_PACKAGES`: Trust all packages for deserialization
- `AUTO_OFFSET_RESET_CONFIG`: Start from earliest message if no offset exists

#### KafkaListenerContainerFactory
```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, RecordCreatedEvent>
        kafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, RecordCreatedEvent> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory());
    return factory;
}
```

**Features:**
- `ConcurrentKafkaListenerContainerFactory`: Allows concurrent message processing
- Sets the consumer factory created above
- Auto-wired into `@KafkaListener` via `containerFactory` attribute

### Deserialization Flow
```
Kafka Message (JSON bytes)
        ↓
JsonDeserializer
        ↓
RecordCreatedEvent object
        ↓
Method parameter (Spring injection)
```

## Application Configuration

### application.yml

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:29092
    consumer:
      group-id: pre-processor-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      auto-offset-reset: earliest
      properties:
        spring.json.trusted.packages: '*'
```

**Configuration:**
- `bootstrap-servers`: Kafka broker address
- `group-id`: Consumer group ID (matches listener)
- `key-deserializer`: Deserialize key as String
- `value-deserializer`: Deserialize value as JSON
- `auto-offset-reset`: Start from earliest if no offset
- `trusted-packages`: Allow deserialization of any package

### Port Configuration
```yaml
server:
  port: 8082
```
- **File Upload Service**: 8081
- **Pre-Processor Service**: 8082
- Allows running both on same machine

## Data Flow Example

### Input Event
```json
{
  "event_id": "550e8400-e29b-41d4-a716-446655440000",
  "event_type": "RecordCreatedEvent",
  "created_at": "2026-06-13T10:30:45.123456",
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

1. **Receive**
   - Kafka delivers message to consumer group
   - `RecordCreatedEventListener.onRecordCreated()` called

2. **Create Tracking**
   - Service creates `PreProcessorTracking` entity:
     ```
     jobId: "JOB1234567890"
     recordId: "REC0987654321"
     memberId: "M001"
     status: "PROCESSING"
     ```

3. **Save to Database**
   - Inserted into `pre_processor_tracking` table
   - ID auto-generated
   - Timestamps auto-set

4. **Route by Preference**
   - Check `communicationPreference`
   - Log routing decision:
     ```
     INFO: Routing record REC0987654321 to EMAIL pipeline
     ```

### Database Result
```
id | job_id | record_id | member_id | status | created_at | updated_at
1  | JOB... | REC...    | M001      | PROCESSING | 2026-06-13... | 2026-06-13...
```

## Error Handling

### Database Errors
- Propagated from `repository.save()`
- Caught by Spring transaction management
- Logged as errors
- Message redelivered by Kafka after backoff

### Invalid Communication Preference
- Logged as WARNING
- Processing continues
- Record saved with unknown preference

### Kafka Deserialization Errors
- Spring logs error
- Message moved to error topic (if configured)
- Or discarded based on Kafka configuration

## Logging Output

### Successful Processing
```
DEBUG c.t.p.k.RecordCreatedEventListener : Received RecordCreatedEvent for recordId: REC0987654321 from jobId: JOB1234567890
DEBUG c.t.p.s.PreProcessorService : Processing record: REC0987654321 from job: JOB1234567890
DEBUG c.t.p.s.PreProcessorService : Saved tracking record for recordId: REC0987654321
INFO  c.t.p.s.PreProcessorService : Routing record REC0987654321 to EMAIL pipeline
```

### Unknown Preference
```
DEBUG c.t.p.k.RecordCreatedEventListener : Received RecordCreatedEvent for recordId: REC0987654321 from jobId: JOB1234567890
DEBUG c.t.p.s.PreProcessorService : Processing record: REC0987654321 from job: JOB1234567890
DEBUG c.t.p.s.PreProcessorService : Saved tracking record for recordId: REC0987654321
WARN  c.t.p.s.PreProcessorService : Unknown communication preference: X for record: REC0987654321
```

## Constraints Met

✅ Create `PreProcessorService` with `processRecord(RecordCreatedEvent)`
✅ Populate fields: jobId, recordId, status, createdAt, updatedAt
✅ Save using Repository
✅ Route by communication preference (E = Email, P = Print)
✅ Log routing decisions
✅ Create Kafka listener on topic `record-created`
✅ Consumer group: `pre-processor-group`
✅ Listener delegates to service
✅ Thin listener, business logic in service
✅ No REST APIs
✅ No Kafka event publishing
✅ No calls to other services
✅ No access to other tables
✅ Minimal, production-quality code

## What's NOT Included

✗ No REST endpoints
✗ No event publishing
✗ No email-service calls
✗ No print-service calls
✗ No other table access
✗ No validation beyond null checks
✗ No async processing
✗ No business logic beyond routing logs

## Production Checklist

- [x] Service layer created with @Service
- [x] Kafka listener created with @KafkaListener
- [x] Consumer configuration defined
- [x] Transaction management with @Transactional
- [x] Proper dependency injection
- [x] Comprehensive logging
- [x] Error handling
- [x] Minimal implementation
- [x] Production-quality code
- [ ] Integration testing (future)
- [ ] E2E testing (future)

## Testing Scenarios

### Scenario 1: Email Preference
```
Event: RecordCreatedEvent(jobId=JOB1, recordId=REC1, 
                          memberId=M001, pref=E)
Result: Record saved, logged "Routing record REC1 to EMAIL pipeline"
```

### Scenario 2: Print Preference
```
Event: RecordCreatedEvent(jobId=JOB1, recordId=REC2, 
                          memberId=M002, pref=P)
Result: Record saved, logged "Routing record REC2 to PRINT pipeline"
```

### Scenario 3: Unknown Preference
```
Event: RecordCreatedEvent(jobId=JOB1, recordId=REC3, 
                          memberId=M003, pref=X)
Result: Record saved, warned "Unknown communication preference: X"
```

### Scenario 4: Multiple Records
```
Event 1: REC1 (pref=E)
Event 2: REC2 (pref=P)
Event 3: REC3 (pref=X)

Result: All 3 records saved and routed independently
```

## Next Steps

1. ✅ Service and listener created
2. Create additional listeners for other services (email-service, print-service)
3. Implement Kafka producer for completion events
4. Add error handling and retries
5. Create monitoring and alerting
6. Add REST APIs for tracking queries

---

**Status**: ✅ Service and Listener Complete - Ready for Integration
