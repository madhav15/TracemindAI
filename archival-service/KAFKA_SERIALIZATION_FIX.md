# Kafka Serialization Fix for ArchivalRequestEvent

## Problem
When the archival-service received an `ArchivalRequestEvent` message that failed processing and was sent to the Dead Letter Topic (DLT), it threw a serialization error:

```
org.apache.kafka.common.errors.SerializationException: Can't convert value of class 
com.tracemindai.common.event.ArchivalRequestEvent to class 
org.apache.kafka.common.serialization.StringSerializer
```

This occurred because:
1. The DLT publishing uses Kafka's default producer settings
2. The default producer serializer was `StringSerializer`
3. But the archival-service was trying to send an `ArchivalRequestEvent` object to the DLT

## Solution

### 1. Created KafkaProducerConfig.java
Added explicit Kafka producer configuration that:
- Uses `JsonSerializer` for the value serializer
- Configures proper `ProducerFactory<String, ArchivalRequestEvent>`
- Creates a `KafkaTemplate` for publishing events

This ensures that when Spring Kafka's `@RetryableTopic` and `@DltHandler` need to publish messages (including to the DLT), they use the correct JSON serializer.

### 2. Updated KafkaConsumerConfig.java
- Added `ErrorHandlingDeserializer` to gracefully handle deserialization errors
- Set ACK mode to `RECORD` for better reliability
- Properly configured the nested deserializers to handle JSON deserialization

### 3. Updated ArchivalRequestEventListener.java
- Added explicit retry configuration with `topicSuffixingStrategy` and `dltStrategy`
- Uses `DltStrategy.FAIL_ON_ERROR` to properly route failed messages to the DLT
- The DLT handler now receives the event and saves it via `DltEventService`

## How It Works

1. Consumer receives `ArchivalRequestEvent` from `archival-request` topic
2. If processing fails, Spring Kafka's retry mechanism kicks in (3 attempts)
3. After all retries are exhausted, the message is sent to the DLT (`archival-request-dlt`)
4. The `@DltHandler` receives the event and saves it to the database for later inspection
5. **Critically**: The producer now uses `JsonSerializer`, so the event object can be properly serialized when sent to the DLT

## Testing

Verify the fix by:
1. Publishing a malformed or problematic `ArchivalRequestEvent` message
2. Confirming the archival-service attempts to process it (up to 3 times)
3. Verifying it's sent to the DLT without serialization errors
4. Checking the `dlt_events` table in PostgreSQL to see the failed message

## Files Modified
- `src/main/java/com/tracemindai/archival/kafka/KafkaProducerConfig.java` (NEW)
- `src/main/java/com/tracemindai/archival/kafka/KafkaConsumerConfig.java` (UPDATED)
- `src/main/java/com/tracemindai/archival/kafka/ArchivalRequestEventListener.java` (UPDATED)
