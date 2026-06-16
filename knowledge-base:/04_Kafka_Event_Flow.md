# 04_Kafka_Event_Flow.md

# TraceMind AI - Kafka Event Flow

## 1. Purpose

This document describes the Kafka-based event-driven architecture used within the TraceMind AI platform.

It explains:

* Event flow between services
* Topic ownership
* Producer and consumer relationships
* Processing sequence
* Retry behavior
* Dead Letter Topic (DLT) handling
* Operational troubleshooting guidance

This document serves as the authoritative reference for Kafka communication across the platform.

---

# 2. Overview

TraceMind AI uses Apache Kafka as the primary communication mechanism between microservices.

Instead of synchronous service-to-service communication, services exchange business events through Kafka topics.

Benefits include:

* Loose coupling
* Independent deployments
* Horizontal scalability
* Improved resiliency
* Better fault isolation
* Event traceability

Every significant business action results in an event that can be traced throughout the processing lifecycle.

---

# 3. Event-Driven Architecture

The platform follows a publish-subscribe model.

A service publishes an event when it completes a business action.

Other services consume that event and continue processing.

```text
Producer Service
        │
        ▼
    Kafka Topic
        │
        ▼
Consumer Service
```

Services are unaware of downstream implementation details and communicate only through event contracts.

---

# 4. Kafka Topic Inventory

The platform currently uses the following business topics:

| Topic Name       | Purpose                      |
| ---------------- | ---------------------------- |
| record-created   | Initiates record processing  |
| email-request    | Requests email processing    |
| print-request    | Requests print processing    |
| archival-request | Requests archival processing |

The platform also supports Dead Letter Topic (DLT) processing for technical failures.

---

# 5. End-to-End Event Flow

```text
file-upload-service
        │
        ▼
record-created
        │
        ▼
pre-processor-service
        │
        ├─────────────► email-request
        │                     │
        │                     ▼
        │               email-service
        │                     │
        │                     ▼
        │              archival-request
        │
        └─────────────► print-request
                              │
                              ▼
                        print-service
                              │
                              ▼
                       archival-request
                              │
                              ▼
                       archival-service
```

This represents the complete business processing workflow.

---

# 6. Topic: record-created

## Purpose

Represents the creation of a new record after successful CSV ingestion.

This event acts as the starting point for downstream processing.

---

## Producer

```text
file-upload-service
```

---

## Consumer

```text
pre-processor-service
```

---

## Business Meaning

A record has been successfully created and is ready for business processing.

---

## Typical Payload Information

```text
jobId
recordId
memberId
correlationId
```

---

## Typical Support Questions

* Was the record-created event published?
* Did pre-processor receive the event?
* Why did processing never start?

---

# 7. Topic: email-request

## Purpose

Requests email processing for a specific record.

Generated when pre-processing determines that email delivery is required.

---

## Producer

```text
pre-processor-service
```

---

## Consumer

```text
email-service
```

---

## Business Meaning

The record requires email-based communication.

---

## Typical Payload Information

```text
jobId
recordId
memberId
email metadata
```

---

## Typical Support Questions

* Was the email request generated?
* Did email-service consume the event?
* Why was email processing not started?

---

# 8. Topic: print-request

## Purpose

Requests print processing for a specific record.

Generated when pre-processing determines that print-based communication is required.

---

## Producer

```text
pre-processor-service
```

---

## Consumer

```text
print-service
```

---

## Business Meaning

The record requires print processing.

---

## Typical Payload Information

```text
jobId
recordId
memberId
print metadata
```

---

## Typical Support Questions

* Was the print request published?
* Did print-service consume the event?
* Why was print generation not initiated?

---

# 9. Topic: archival-request

## Purpose

Requests final archival processing after successful email or print completion.

This topic represents the final business stage before processing completion.

---

## Producers

```text
email-service

print-service
```

---

## Consumer

```text
archival-service
```

---

## Business Meaning

Processing has completed successfully and artifacts should now be archived.

---

## Typical Payload Information

```text
jobId
recordId
memberId
processing outcome
```

---

## Typical Support Questions

* Was archival requested?
* Why was archival not completed?
* Did archival-service receive the event?

---

# 10. Event Processing Sequence

## Email Path

```text
record-created
        │
        ▼
pre-processor-service
        │
        ▼
email-request
        │
        ▼
email-service
        │
        ▼
archival-request
        │
        ▼
archival-service
```

---

## Print Path

```text
record-created
        │
        ▼
pre-processor-service
        │
        ▼
print-request
        │
        ▼
print-service
        │
        ▼
archival-request
        │
        ▼
archival-service
```

Only one path is selected for a record.

A record follows either the Email Path or the Print Path.

---

# 11. Event Correlation Strategy

Events are correlated using business identifiers.

Primary identifiers include:

```text
jobId
recordId
memberId
```

Additional tracing identifiers:

```text
correlationId
traceId
```

These identifiers allow support teams to reconstruct complete processing journeys across multiple services.

---

# 12. Retry Strategy

The platform distinguishes between business failures and technical failures.

---

## Business Failures

Examples:

* Invalid member data
* Missing mandatory information
* Business rule violations

Behavior:

```text
No Retry
No DLT
```

Reason:

Repeated execution will not resolve the issue.

---

## Technical Failures

Examples:

* Kafka unavailable
* Database unavailable
* SMTP unavailable
* Network timeout
* Service interruption

Behavior:

```text
Attempt 1
    │
Attempt 2
    │
Attempt 3
    │
    ▼
Dead Letter Topic
```

Technical failures are retried automatically before escalation.

---

# 13. Dead Letter Topic (DLT)

## Purpose

Stores events that could not be processed successfully after retry exhaustion.

The DLT acts as an operational safety mechanism.

---

## Entry Criteria

An event is moved to DLT when:

* Maximum retry count is reached
* Processing still fails
* Recovery is not possible automatically

---

## Typical Causes

* Persistent infrastructure failures
* Corrupted payloads
* Downstream dependency outages
* Configuration issues

---

## Operational Value

DLT events help support teams:

* Identify processing bottlenecks
* Investigate failures
* Perform manual recovery
* Prevent message loss

---

# 14. Kafka and ProcessLog Integration

Every Kafka processing stage emits structured ProcessLog events.

Example lifecycle:

```text
Event Published

Event Consumed

Processing Started

Processing Completed

or

Processing Failed
```

These logs are written to:

```text
process.log
```

and indexed by Splunk.

This enables complete operational visibility across the platform.

---

# 15. Common Support Investigation Patterns

## Event Never Processed

Questions:

```text
Why was the record not processed?
```

Investigation:

1. Verify event publication.
2. Verify consumer activity.
3. Verify processing logs.
4. Check retry events.
5. Check DLT events.

---

## Service Failure

Questions:

```text
Why did email-service fail?
```

Investigation:

1. Review email-service logs.
2. Review retry attempts.
3. Review DLT activity.
4. Verify dependency availability.

---

## Missing Archival

Questions:

```text
Why was the record not archived?
```

Investigation:

1. Verify archival-request publication.
2. Verify archival-service consumption.
3. Review archival logs.
4. Check retry history.

---

# 16. Design Principles

### Event-Driven Communication

Services communicate through events rather than direct calls.

---

### Loose Coupling

Services remain independent of downstream implementations.

---

### Fault Isolation

Failures in one service do not immediately impact other services.

---

### Traceability

Every event can be traced through business identifiers and ProcessLog entries.

---

### Reliability

Retry and DLT mechanisms prevent message loss and improve resilience.

---

# 17. Summary

Kafka serves as the communication backbone of the TraceMind AI platform.

All business processing is driven by events flowing through Kafka topics.

Each topic represents a business milestone and connects independently deployable services.

Combined with structured ProcessLog events, Kafka provides complete traceability, resiliency, and operational visibility across the platform, making it possible for Splunk and MCP-based investigations to reconstruct end-to-end processing journeys accurately.
