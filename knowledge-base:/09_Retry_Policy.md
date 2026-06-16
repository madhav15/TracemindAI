# 09_Retry_Policy.md

# TraceMind AI - Retry Policy

## 1. Purpose

This document defines the retry strategy used throughout the TraceMind AI platform.

The retry policy ensures that transient technical failures are handled automatically while preventing unnecessary processing of non-recoverable failures.

This document serves as the authoritative reference for:

* Retry behavior
* Failure classification
* Technical failure handling
* DLT escalation
* Operational troubleshooting

The policy applies to all Kafka-based asynchronous processing within the platform.

---

# 2. Objectives

The retry policy is designed to achieve the following goals:

### Improve Reliability

Automatically recover from temporary failures.

---

### Reduce Manual Intervention

Minimize operational support effort.

---

### Prevent Message Loss

Ensure events are not silently discarded.

---

### Protect Downstream Systems

Avoid excessive retry storms.

---

### Maintain Processing Integrity

Ensure failed events are handled consistently.

---

# 3. Core Principle

The TraceMind AI platform distinguishes between:

```text
Business Failures
```

and

```text
Technical Failures
```

Only technical failures are eligible for retries.

This distinction is fundamental to the platform's error handling strategy.

---

# 4. Failure Classification

## Business Failures

Business failures occur when the data itself is invalid or violates business rules.

Examples:

```text
Invalid Member Data

Missing Mandatory Fields

Invalid Email Address

Business Rule Violation

Unsupported Processing Type
```

### Characteristics

* Deterministic
* Repeatable
* Non-recoverable through retry

### Retry Behavior

```text
No Retry
```

### DLT Behavior

```text
No DLT
```

### Reason

Repeating execution will produce the same outcome.

Retrying would consume resources without improving success rates.

---

## Technical Failures

Technical failures occur when processing cannot complete because of infrastructure, platform, or dependency issues.

Examples:

```text
Database Unavailable

SMTP Timeout

Kafka Connectivity Failure

Network Timeout

Service Unavailable

External Dependency Failure
```

### Characteristics

* Temporary
* Recoverable
* Infrastructure-related

### Retry Behavior

```text
Retry Enabled
```

### DLT Behavior

```text
DLT After Retry Exhaustion
```

### Reason

The underlying issue may resolve without human intervention.

---

# 5. Standard Retry Strategy

The platform follows a fixed retry strategy.

```text
Initial Attempt
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
Dead Letter Topic
```

---

## Maximum Retry Count

```text
3
```

---

## Total Processing Attempts

```text
4
```

Calculation:

```text
Initial Attempt

+

3 Retries
```

---

# 6. Retry Lifecycle

The following sequence illustrates the complete retry process.

```text
Message Consumed
       │
       ▼
Processing Started
       │
       ▼
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

Each retry attempt generates ProcessLog events.

---

# 7. Retry Eligibility Rules

A retry may occur only when all conditions are satisfied.

### Condition 1

The failure is technical.

---

### Condition 2

Maximum retry count has not been reached.

---

### Condition 3

The message payload remains valid.

---

### Condition 4

The event has not already been moved to DLT.

---

If any condition fails, processing stops.

---

# 8. Non-Retryable Errors

The following categories should never be retried.

### Validation Errors

Examples:

```text
Missing Required Fields

Invalid Data Format
```

---

### Business Rule Violations

Examples:

```text
Member Not Eligible

Unsupported Request Type
```

---

### Data Integrity Violations

Examples:

```text
Corrupted Business Data
```

---

### Permanent Configuration Errors

Examples:

```text
Invalid Business Mapping
```

---

## Rationale

Retries cannot correct these conditions.

The issue must be resolved through data correction or application changes.

---

# 9. Retryable Errors

The following categories are typically retryable.

### Infrastructure Failures

Examples:

```text
Database Connection Failure

Connection Pool Exhaustion
```

---

### Messaging Failures

Examples:

```text
Kafka Timeout

Broker Unavailable
```

---

### Network Failures

Examples:

```text
Socket Timeout

Temporary Connectivity Loss
```

---

### External Service Failures

Examples:

```text
SMTP Service Unavailable

Third-Party Service Timeout
```

---

## Rationale

These failures are often temporary and may resolve automatically.

---

# 10. ProcessLog Representation

Retry activity must be visible through ProcessLog events.

Typical retry event:

```json
{
  "service":"email-service",
  "status":"RETRY",
  "retryCount":1,
  "errorType":"SMTP_TIMEOUT",
  "message":"Retry attempt initiated"
}
```

---

## Required Fields

Retry events should include:

```text
service

jobId

recordId

retryCount

errorType

errorMessage
```

These fields support operational investigations.

---

# 11. Splunk Visibility

Retry events are indexed within Splunk.

Support teams can identify retries using:

```text
status=RETRY
```

or

```text
retryCount > 0
```

---

## Typical Investigation Questions

* Which service retried?
* How many retries occurred?
* What caused the retry?
* Was recovery successful?

---

# 12. MCP Tool Support

Retry analysis is exposed through:

```text
get_retry_events
```

This MCP tool retrieves retry-related operational facts from Splunk.

---

## Example Questions

```text
Show retry events for JOB-1001

Why did email-service retry?

How many retries occurred?
```

---

# 13. Retry Outcomes

Every retry sequence results in one of two outcomes.

---

## Outcome 1 - Recovery

```text
Failure
   │
Retry
   │
Success
```

Processing resumes normally.

No DLT event is generated.

---

## Outcome 2 - Exhaustion

```text
Failure
   │
Retry
   │
Retry
   │
Retry
   │
DLT
```

Processing is terminated.

The event is moved to the Dead Letter Topic.

---

# 14. Relationship with DLT Policy

Retry and DLT handling are closely related.

Retry attempts occur first.

DLT processing occurs only after retry exhaustion.

Relationship:

```text
Technical Failure
       │
       ▼
Retry Processing
       │
       ▼
Retry Exhausted
       │
       ▼
Dead Letter Topic
```

The DLT Policy document defines post-retry handling procedures.

---

# 15. Operational Guidance

Support engineers should always review retry history before escalating an issue.

Recommended investigation sequence:

```text
Timeline Review
      │
      ▼
Retry Review
      │
      ▼
DLT Review
      │
      ▼
Root Cause Analysis
```

Retry data often provides critical clues regarding infrastructure instability.

---

# 16. Common Investigation Scenarios

## Scenario 1

Question:

```text
Why did processing take longer than expected?
```

Possible Cause:

Multiple retry attempts occurred before success.

---

## Scenario 2

Question:

```text
Why did email delivery eventually succeed?
```

Possible Cause:

Temporary SMTP outage recovered during retries.

---

## Scenario 3

Question:

```text
Why did the message enter DLT?
```

Possible Cause:

All retry attempts failed.

---

# 17. Design Principles

### Retry Only Technical Failures

Business failures are not retryable.

---

### Keep Retry Logic Predictable

Use a fixed retry count.

---

### Maintain Observability

Every retry must be visible through ProcessLog events.

---

### Avoid Retry Storms

Limit retries to prevent infrastructure overload.

---

### Preserve Explainability

Support teams must be able to understand retry decisions.

---

# 18. Summary

The TraceMind AI Retry Policy provides a consistent and predictable approach for handling transient technical failures.

The platform performs up to three retry attempts for eligible technical failures before escalating the event to a Dead Letter Topic.

Business failures are never retried because repeated execution cannot resolve data or rule-related issues.

This strategy improves reliability, reduces manual intervention, prevents message loss, and provides clear operational visibility through ProcessLog events, Splunk investigations, and MCP-based support tooling.
