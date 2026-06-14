# Kafka Integration Guide

## Overview

The File Upload Service publishes `RecordCreatedEvent` events to Kafka after each record is successfully persisted to the database.

## Kafka Configuration

### Topic

**Name**: `record-created`  
**Partitions**: Default (configurable)  
**Replication Factor**: Default (configurable)

### Broker Configuration

Default configuration in `application.yml`:

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 0
```

**Key Configuration Options**:
- `bootstrap-servers`: Kafka broker address (default: localhost:9092)
- `acks`: all (waits for all in-sync replicas to acknowledge)
- `retries`: 0 (no retries on failure per requirements)

## Event Publishing Flow

```
CSV Record Persisted
        ↓
RecordCreatedEvent Created
        ↓
RecordProducer.publish(event)
        ↓
Kafka Topic: record-created
        ↓
Consumer (external)
```

## RecordCreatedEvent Structure

```java
{
  "event_id": "UUID",           // From DomainEvent
  "event_type": "RecordCreatedEvent",  // From DomainEvent
  "created_at": "ISO timestamp",       // From DomainEvent
  "source": "file-upload-service",     // From DomainEvent
  "job_id": "JOB123456",        // Job identifier
  "record_id": "REC789012",     // Record identifier
  "member_id": "M001",          // Member ID from CSV
  "communication_preference": "EMAIL",  // Communication preference
  "correlation_id": "JOB123456",// Matches jobId (for tracing)
  "trace_id": "REC789012"       // Matches recordId (for tracing)
}
```

### Event Fields

| Field | Type | Source | Purpose |
|-------|------|--------|---------|
| jobId | String | Job entity | Identifies the upload job |
| recordId | String | Record entity | Unique record identifier |
| memberId | String | CSV data | Member identifier |
| communicationPreference | String | CSV data | Contact preference (EMAIL, SMS, PHONE, BOTH) |
| correlationId | String | jobId | Track events from same job |
| traceId | String | recordId | Track individual record |
| eventId | String | Auto-generated | Unique event ID |
| eventType | String | DomainEvent | Event class name |
| createdAt | LocalDateTime | Auto-generated | Event creation timestamp |
| source | String | DomainEvent | Service name |

## Code Components

### RecordProducer

**Location**: `kafka/RecordProducer.java`

```java
@Component
@RequiredArgsConstructor
public class RecordProducer {
    private final KafkaTemplate<String, RecordCreatedEvent> kafkaTemplate;

    public void publish(RecordCreatedEvent event) {
        // Publishes to "record-created" topic
        // Uses recordId as message key
    }
}
```

**Key Features**:
- Publishes `RecordCreatedEvent` to `record-created` topic
- Uses `recordId` as message key for partitioning
- Catches and logs publishing exceptions
- Throws `KafkaPublishException` on failure

### KafkaProducerConfig

**Location**: `kafka/KafkaProducerConfig.java`

Configures:
- `ProducerFactory<String, RecordCreatedEvent>` - Creates producer instances
- `KafkaTemplate<String, RecordCreatedEvent>` - Spring wrapper for Kafka sending

### KafkaPublishException

**Location**: `kafka/KafkaPublishException.java`

Custom exception thrown when Kafka publishing fails. Propagates to controller's global exception handler.

## Integration with File Upload Service

### Modified FileUploadService

```java
@Transactional
public FileUploadResponse uploadCsv(MultipartFile file) {
    // 1. Parse CSV
    List<MemberRecord> memberRecords = csvParser.parse(file);
    
    // 2. Create Job
    Job job = createJob(file.getOriginalFilename(), memberRecords.size());
    
    // 3. Create Records and Publish Events
    createRecords(job.getJobId(), memberRecords);  // Each record publishes event
    
    // 4. Return response
    return buildResponse(job);
}
```

### Record Creation with Event Publishing

```java
private void createRecords(String jobId, List<MemberRecord> memberRecords) {
    for (MemberRecord memberRecord : memberRecords) {
        String recordId = getRecordId();
        
        // 1. Persist Record
        Record record = createRecord(...);
        recordRepository.save(record);
        
        // 2. Publish Event
        publishRecordCreatedEvent(jobId, recordId, memberRecord);
    }
}

private void publishRecordCreatedEvent(
        String jobId, String recordId, MemberRecord memberRecord) {
    RecordCreatedEvent event = RecordCreatedEvent.builder()
            .jobId(jobId)
            .recordId(recordId)
            .memberId(memberRecord.getMemberId())
            .communicationPreference(memberRecord.getCommunicationPreference())
            .correlationId(jobId)      // For job-level tracing
            .traceId(recordId)          // For record-level tracing
            .build();
    
    recordProducer.publish(event);
}
```

## Setup and Configuration

### Prerequisites

1. **Kafka Running**
   ```bash
   # Start Kafka broker on localhost:9092
   kafka-server-start.sh config/server.properties
   ```

2. **Create Topic** (optional - can be auto-created)
   ```bash
   kafka-topics.sh --create \
     --bootstrap-server localhost:9092 \
     --topic record-created \
     --partitions 3 \
     --replication-factor 1
   ```

### Application Configuration

Update `application.yml` for your environment:

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092  # Update for your Kafka broker
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 0
```

### Environment Variables

```bash
export SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka-broker:9092
export SPRING_KAFKA_PRODUCER_ACKS=all
```

## Monitoring

### Published Events

Monitor events being published:

```bash
# Consume from record-created topic
kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic record-created \
  --from-beginning \
  --property print.key=true
```

### Application Logs

Check logs for event publishing:

```
DEBUG - Published RecordCreatedEvent for recordId: REC789012 to topic: record-created
```

### Event Verification

Example event published to Kafka:

```json
{
  "event_id": "550e8400-e29b-41d4-a716-446655440000",
  "event_type": "RecordCreatedEvent",
  "created_at": "2026-06-13T10:30:45.123456",
  "source": "file-upload-service",
  "job_id": "JOB1234567890",
  "record_id": "REC0987654321",
  "member_id": "M001",
  "communication_preference": "EMAIL",
  "correlation_id": "JOB1234567890",
  "trace_id": "REC0987654321"
}
```

## Error Handling

### Publishing Failures

If Kafka publishing fails:

1. **Exception Caught**: `KafkaPublishException` thrown
2. **Logged**: Error logged with recordId context
3. **Propagated**: Exception propagates to controller
4. **Response**: HTTP 500 error returned to client
5. **Database**: Record already persisted (no rollback)

**Note**: No retries are configured. In case of failure, the event is lost.

### Kafka Unavailable

If Kafka broker is unavailable:

```
ERROR - Failed to publish RecordCreatedEvent for recordId: REC789012
Exception: org.apache.kafka.common.KafkaException: Failed to construct kafka producer
```

The upload will fail with HTTP 500 error.

## Constraints Met

✅ Uses Spring Kafka for producer integration  
✅ Publishes to `record-created` topic  
✅ Uses `RecordCreatedEvent` from common-lib  
✅ `RecordProducer` component with `publish()` method  
✅ Events published after record persisted  
✅ Populates all required event fields  
✅ jobId as correlationId, recordId as traceId  
✅ No Kafka consumer implementation  
✅ No additional topics created  
✅ No retries configured (retries: 0)  
✅ No DLQ (Dead Letter Queue)  
✅ Synchronous publishing (no async)  
✅ REST API unchanged  
✅ Business logic unchanged  

## Testing

### 1. Start Kafka

```bash
# Terminal 1: Start Kafka broker
kafka-server-start.sh config/server.properties
```

### 2. Start Application

```bash
# Terminal 2: Start file-upload-service
cd /Users/madhav/Projects/java/TracemindAI/file-upload-service
mvn spring-boot:run
```

### 3. Consume Events

```bash
# Terminal 3: Monitor events
kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic record-created \
  --from-beginning \
  --property print.key=true
```

### 4. Upload File

```bash
# Terminal 4: Upload CSV
curl -X POST http://localhost:8081/api/jobs/upload \
  -F "file=@sample-members.csv"
```

### 5. Verify Events

In Terminal 3, you should see 10 RecordCreatedEvent messages published to Kafka.

## Dependencies

Added to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

Provides:
- `KafkaTemplate` for sending messages
- `KafkaProducerFactory` for producer configuration
- Automatic deserialization/serialization
- Spring Boot auto-configuration

## Performance Considerations

### Synchronous Publishing

Events are published synchronously within the same transaction context:
- Each record save → Event publish → Move to next record
- Latency depends on Kafka broker response time
- If Kafka is slow, upload will be slow

### Partitioning

Records are distributed across partitions using `recordId` as key:
- Each record has unique key (recordId)
- Enables parallel event processing by consumers
- Maintains order within each partition

### Batch Operations

Note: Changed from batch `saveAll()` to individual `save()` for each record to enable event publishing after each save.

## Future Enhancements

Potential improvements (not implemented per requirements):

1. **Async Publishing**: Use `@Async` for non-blocking publishing
2. **Retry Logic**: Add exponential backoff on failures
3. **DLQ**: Route failed messages to Dead Letter Queue
4. **Consumer**: Add event consumer for downstream processing
5. **Metrics**: Add Micrometer metrics for monitoring
6. **Idempotency**: Add idempotency keys to prevent duplicates
7. **Schema Registry**: Use Confluent Schema Registry for schema management

## Troubleshooting

### Kafka Broker Connection Failed

```
ERROR o.s.k.c.DefaultKafkaProducerFactory : Error creating Kafka producer
Exception: org.apache.kafka.common.KafkaException: 
  Failed to construct kafka producer
```

**Solution**: Verify Kafka broker is running on configured bootstrap servers.

### Topic Does Not Exist

Kafka auto-creates topics with default settings. No action needed.

### Events Not Appearing in Consumer

1. Check broker connection: `kafka-broker-api-versions.sh --bootstrap-server localhost:9092`
2. Verify topic exists: `kafka-topics.sh --list --bootstrap-server localhost:9092`
3. Check application logs for publishing errors
4. Verify CSV upload completed successfully (check HTTP response)

### High Memory Usage

Each event is serialized to JSON. For large batch uploads:
- Kafka producer buffers messages in memory
- Default buffer size: 32MB
- Adjust `spring.kafka.producer.batch-size` if needed

## References

- [Spring Kafka Documentation](https://spring.io/projects/spring-kafka)
- [Apache Kafka Producer Config](https://kafka.apache.org/documentation/#producerconfigs)
- [RecordCreatedEvent (common-lib)](../../common-lib/src/main/java/com/tracemindai/common/event/RecordCreatedEvent.java)

---

**Status**: ✅ Kafka Integration Complete
