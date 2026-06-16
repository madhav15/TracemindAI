# 10_DLT_Policy.md

# TraceMind AI - Dead Letter Topic (DLT) Policy

## 1. Purpose

This document defines the Dead Letter Topic (DLT) strategy used within the TraceMind AI platform.

The DLT mechanism provides a controlled approach for handling messages that cannot be processed successfully after retry exhaustion.

This document serves as the authoritative reference for:

* DLT processing
* Retry exhaustion handling
* Failure escalation
* Operational investigations
* Message recovery procedures
* Support workflows

The policy applies to all Kafka-based message processing within the platform.

---

# 2. Overview

In distributed systems, some failures cannot be resolved automatically even after multiple retry attempts.

Without a DLT strategy, these failures can result in:

* Message loss
* Infinite retry loops
* Processing bottlenecks
* Operational instability
* Reduced system reliability

The Dead Letter Topic provides a safe destination for failed messages while preserving the information required for investigation and recovery.

---

# 3. Definition

A Dead Letter Topic (DLT) is a Kafka topic that stores messages that could not be processed successfully after all retry attempts have been exhausted.

A message enters DLT only after:

```text
Technical Failure
       │
       ▼
Retry Attempt 1
       │
       ▼
Retry Attempt 2
       │
       ▼
Retry Attempt 3
       │
       ▼
DLT
```

The DLT acts as the final safety mechanism within the message processing lifecycle.

---

# 4. Objectives

The DLT strategy is designed to achieve the following goals.

### Prevent Message Loss

Retain failed messages for analysis.

---

### Protect Platform Stability

Avoid endless retry cycles.

---

### Improve Observability

Provide visibility into persistent failures.

---

### Support Recovery

Enable investigation and reprocessing.

---

### Improve Supportability

Provide a clear escalation path for unresolved issues.

---

# 5. Relationship with Retry Policy

The DLT Policy is dependent upon the Retry Policy.

Processing sequence:

```text
Message Received
       │
       ▼
Processing Attempt
       │
       ▼
Technical Failure
       │
       ▼
Retry Processing
       │
       ▼
Retry Exhausted
       │
       ▼
DLT
```

A message cannot enter DLT without first exhausting the configured retry policy.

---

# 6. DLT Entry Criteria

A message may enter DLT only when all of the following conditions are met.

### Condition 1

The failure is classified as a technical failure.

---

### Condition 2

All retry attempts have been exhausted.

---

### Condition 3

Processing remains unsuccessful.

---

### Condition 4

Automatic recovery is no longer possible.

---

If any condition is not satisfied, the message should not be moved to DLT.

---

# 7. Non-DLT Scenarios

The following conditions should never result in DLT processing.

---

## Business Validation Failures

Examples:

```text
Missing Required Data

Invalid Business Rules

Unsupported Business Scenario
```

Reason:

These failures are not retryable.

---

## User Data Issues

Examples:

```text
Invalid Member Information

Invalid Email Address
```

Reason:

Data correction is required.

---

## Functional Rejections

Examples:

```text
Business Eligibility Failure

Policy Violation
```

Reason:

The application intentionally rejected the request.

---

# 8. Typical DLT Scenarios

The following situations commonly result in DLT events.

---

## Database Unavailable

Example:

```text
Database Connection Failure
```

All retry attempts fail.

---

## SMTP Service Unavailable

Example:

```text
SMTP Timeout
```

Email delivery remains unavailable.

---

## Kafka Infrastructure Failure

Example:

```text
Broker Connectivity Failure
```

Message processing cannot continue.

---

## Persistent Network Failure

Example:

```text
Network Timeout
```

External dependencies remain unreachable.

---

## External Dependency Outage

Example:

```text
Third-Party Service Down
```

The dependency does not recover during retry attempts.

---

# 9. DLT Lifecycle

The complete DLT lifecycle is illustrated below.

```text
Event Published
       │
       ▼
Event Consumed
       │
       ▼
Processing Started
       │
       ▼
Technical Failure
       │
       ▼
Retry 1
       │
       ▼
Retry 2
       │
       ▼
Retry 3
       │
       ▼
DLT
       │
       ▼
Investigation
       │
       ▼
Resolution
```

This provides a complete audit trail for operational support.

---

# 10. DLT Event Logging

When a message enters DLT, a ProcessLog event must be generated.

Example:

```json
{
  "service":"email-service",
  "status":"DLT",
  "jobId":"JOB-1001",
  "recordId":"REC-2001",
  "errorType":"SMTP_TIMEOUT",
  "retryCount":3,
  "message":"Message moved to DLT after retry exhaustion"
}
```

---

# 11. Required DLT Fields

DLT events should contain the following information.

```text
service

jobId

recordId

memberId

errorType

errorMessage

retryCount

correlationId

traceId
```

These fields are required for investigation and recovery.

---

# 12. Splunk Visibility

DLT events are indexed into Splunk.

Support teams can identify DLT events using:

```text
status=DLT
```

---

## Typical Investigation Questions

* Which records entered DLT?
* Which service generated the DLT event?
* What caused processing failure?
* How many retry attempts occurred?
* Is recovery possible?

---

# 13. MCP Tool Support

DLT analysis is exposed through:

```text
get_dlt_events
```

This tool retrieves DLT-related operational facts from Splunk.

---

## Example Questions

```text
Show DLT events for JOB-1001

Why did the message enter DLT?

Which records are currently in DLT?

Show failed processing events
```

---

# 14. Operational Investigation Workflow

When a DLT event is detected, the following investigation process should be followed.

```text
Identify DLT Event
       │
       ▼
Review Timeline
       │
       ▼
Review Retry History
       │
       ▼
Identify Failure Cause
       │
       ▼
Determine Recovery Action
       │
       ▼
Resolve Issue
```

This workflow should be applied consistently.

---

# 15. Recovery Strategy

The appropriate recovery action depends on the root cause.

---

## Infrastructure Recovery

Examples:

```text
Database Restored

SMTP Available

Network Recovered
```

Action:

Reprocess affected messages.

---

## Configuration Recovery

Examples:

```text
Incorrect Configuration

Missing Credentials
```

Action:

Correct configuration and reprocess.

---

## Dependency Recovery

Examples:

```text
Third-Party Service Restored
```

Action:

Resume processing after dependency stabilization.

---

# 16. Escalation Guidelines

Escalation should occur when:

### High DLT Volume

May indicate a platform-wide issue.

---

### Repeated DLT Events

May indicate an unresolved systemic problem.

---

### Unknown Failure Pattern

Requires engineering analysis.

---

### Infrastructure Instability

Requires platform support involvement.

---

### Data Corruption

Requires immediate engineering investigation.

---

# 17. Operational Metrics

Support teams should monitor:

### DLT Volume

Number of messages entering DLT.

---

### DLT Growth Rate

Rate of increase over time.

---

### Top Failing Services

Services generating the most DLT events.

---

### Common Error Types

Most frequent failure categories.

---

### Recovery Success Rate

Percentage of recovered DLT events.

---

# 18. Common Investigation Examples

## Scenario 1

Question:

```text
Why did JOB-1001 fail?
```

Investigation:

```text
get_job_summary
       │
get_retry_events
       │
get_dlt_events
```

Determine whether retry exhaustion occurred.

---

## Scenario 2

Question:

```text
Why did email processing stop?
```

Investigation:

Review email-service DLT events.

Identify error type.

Determine dependency status.

---

## Scenario 3

Question:

```text
Why did processing never complete?
```

Investigation:

Check for DLT events associated with the record.

Review retry history.

Identify final failure reason.

---

# 19. Design Principles

### Never Lose Messages

Failed messages must be preserved.

---

### Prevent Infinite Retries

Retries must be bounded.

---

### Maintain Full Traceability

All DLT events must be logged.

---

### Support Root Cause Analysis

DLT records must contain sufficient diagnostic information.

---

### Enable Recovery

Failed messages should remain available for investigation and reprocessing.

---

# 20. Summary

The Dead Letter Topic (DLT) Policy provides the final safety mechanism within the TraceMind AI event processing architecture.

Messages enter DLT only after exhausting the configured retry policy and failing to recover from technical issues.

The DLT preserves failed messages, supports operational investigations, prevents infinite retry loops, and enables controlled recovery procedures.

Combined with ProcessLog, Splunk, and MCP tooling, the DLT strategy provides complete visibility into persistent processing failures and forms a critical component of the platform's reliability and supportability model.
