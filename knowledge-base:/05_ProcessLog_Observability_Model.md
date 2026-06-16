# 05_ProcessLog_Observability_Model.md

# TraceMind AI - ProcessLog Observability Model

## 1. Purpose

This document defines the ProcessLog observability model used throughout the TraceMind AI platform.

ProcessLog is the foundational mechanism that enables:

* End-to-end traceability
* Production support investigations
* Splunk-based analytics
* Timeline reconstruction
* Failure analysis
* Retry tracking
* AI-assisted troubleshooting

Every business service emits structured ProcessLog events that capture the state of processing at key stages of the workflow.

These logs form the authoritative operational record consumed by Splunk and exposed through MCP tools.

---

# 2. Overview

Traditional application logs are often difficult to search, correlate, and analyze across distributed systems.

TraceMind AI addresses this challenge by standardizing all operational logging through a structured JSON-based ProcessLog model.

Each log entry represents a meaningful business or technical event.

Examples:

* Job creation
* Record creation
* Event publication
* Event consumption
* Processing started
* Processing completed
* Processing failed
* Retry attempted
* Message moved to DLT

This approach enables consistent analysis across all services.

---

# 3. Design Objectives

The ProcessLog model was designed to achieve the following goals:

### Traceability

Track processing across multiple services.

---

### Searchability

Enable efficient Splunk searches.

---

### Consistency

Standardize logging across all services.

---

### Explainability

Allow support teams to understand what happened and why.

---

### AI Readiness

Provide structured operational facts for MCP tools and AI-driven investigations.

---

# 4. Logging Architecture

```text
Business Service
        │
        ▼
   ProcessLog Event
        │
        ▼
    process.log
        │
        ▼
Splunk File Monitor
        │
        ▼
   tracemind Index
        │
        ▼
     MCP Tools
        │
        ▼
 Claude Desktop
```

Every service follows the same logging model.

No service writes directly to Splunk.

Splunk monitors the generated log file and indexes the events.

---

# 5. ProcessLog Structure

Each ProcessLog entry is stored as a single JSON document.

Example:

```json
{
  "timestamp":"2026-06-16T10:15:22Z",
  "service":"email-service",
  "stage":"EMAIL_PROCESSING",
  "action":"SEND_EMAIL",
  "status":"SUCCESS",
  "jobId":"JOB-1001",
  "recordId":"REC-2001",
  "memberId":"MEM-5001",
  "correlationId":"CORR-123",
  "traceId":"TRACE-456",
  "message":"Email sent successfully",
  "errorType":null,
  "errorMessage":null,
  "retryCount":0
}
```

Each log entry should represent one meaningful operational event.

---

# 6. Standard Fields

## timestamp

### Purpose

Identifies when the event occurred.

### Example

```text
2026-06-16T10:15:22Z
```

### Usage

Used for:

* Timeline reconstruction
* Duration calculations
* Event ordering

---

## service

### Purpose

Identifies the service that generated the log.

### Examples

```text
file-upload-service

pre-processor-service

email-service

print-service

archival-service
```

### Usage

Used for service-specific investigations.

---

## stage

### Purpose

Represents the business processing stage.

### Examples

```text
FILE_UPLOAD

RECORD_CREATION

PRE_PROCESSING

EMAIL_PROCESSING

PRINT_PROCESSING

ARCHIVAL
```

### Usage

Used to understand where processing occurred.

---

## action

### Purpose

Describes the specific activity performed.

### Examples

```text
CREATE_JOB

CREATE_RECORD

PUBLISH_EVENT

SEND_EMAIL

GENERATE_PRINT

ARCHIVE_RECORD
```

### Usage

Provides granular operational detail.

---

## status

### Purpose

Represents the outcome of an action.

### Allowed Values

```text
SUCCESS

FAILED

IN_PROGRESS

RETRY

DLT
```

### Usage

Used heavily during support investigations.

---

## jobId

### Purpose

Business identifier for file-level processing.

### Example

```text
JOB-1001
```

### Usage

Used to reconstruct complete job journeys.

---

## recordId

### Purpose

Business identifier for record-level processing.

### Example

```text
REC-2001
```

### Usage

Used to trace individual records.

---

## memberId

### Purpose

Business identifier representing the member being processed.

### Example

```text
MEM-5001
```

### Usage

Provides business-level traceability.

---

## correlationId

### Purpose

Links related events across services.

### Example

```text
CORR-123
```

### Usage

Used to correlate a processing journey.

---

## traceId

### Purpose

Tracks distributed execution flow.

### Example

```text
TRACE-456
```

### Usage

Supports distributed tracing scenarios.

---

## message

### Purpose

Human-readable description of the event.

### Examples

```text
Email sent successfully

Record archived

Retry attempt initiated
```

### Usage

Provides context during investigations.

---

## errorType

### Purpose

Categorizes failures.

### Examples

```text
SMTP_TIMEOUT

DATABASE_ERROR

KAFKA_ERROR
```

### Usage

Supports failure analysis.

---

## errorMessage

### Purpose

Stores detailed failure information.

### Example

```text
Connection timeout after 30 seconds
```

### Usage

Supports root cause investigations.

---

## retryCount

### Purpose

Indicates the current retry attempt.

### Examples

```text
0
1
2
3
```

### Usage

Supports retry analysis and DLT investigations.

---

# 7. Event Lifecycle Example

The following sequence illustrates a typical record journey.

```text
Record Created
       │
       ▼
Event Published
       │
       ▼
Event Consumed
       │
       ▼
Processing Started
       │
       ▼
Processing Completed
```

Each step generates a ProcessLog event.

---

# 8. Failure Lifecycle Example

A technical failure may generate the following sequence.

```text
Processing Started
       │
       ▼
Processing Failed
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
Moved To DLT
```

This provides a complete audit trail for operational analysis.

---

# 9. Job Timeline Reconstruction

Support engineers frequently investigate processing at the job level.

Example:

```text
JOB-1001
```

Splunk can reconstruct the entire journey using ProcessLog entries containing the same jobId.

Typical timeline:

```text
10:00 Job Created

10:01 Records Created

10:02 Pre-Processing Started

10:03 Email Sent

10:04 Archived

10:04 Completed
```

This forms the basis of the get_job_timeline MCP tool.

---

# 10. Record Timeline Reconstruction

Record investigations follow the same pattern.

Example:

```text
REC-2001
```

All ProcessLog entries containing the recordId can be assembled into a complete lifecycle view.

This forms the basis of the get_record_timeline MCP tool.

---

# 11. Retry Analysis

Retry events are identified through:

```text
status = RETRY
```

and

```text
retryCount > 0
```

Typical investigation questions:

* How many retries occurred?
* Which service retried?
* What caused the retry?
* Was processing eventually successful?

This forms the basis of the get_retry_events MCP tool.

---

# 12. DLT Analysis

DLT events are identified through:

```text
status = DLT
```

These entries indicate that retry attempts were exhausted.

Typical investigation questions:

* Which records entered DLT?
* Why did processing fail?
* Which service generated the DLT event?

This forms the basis of the get_dlt_events MCP tool.

---

# 13. Splunk Integration

All ProcessLog entries are written to:

```text
process.log
```

Splunk monitors the log file and indexes entries into:

```text
tracemind
```

index.

The indexed data becomes the authoritative source for operational investigations.

---

# 14. MCP Tool Dependency

Most MCP tools depend directly on ProcessLog data.

Examples:

### Job Analysis

* get_job_timeline
* get_job_summary

### Record Analysis

* get_record_timeline

### Failure Analysis

* get_failed_jobs
* get_failed_emails

### Retry Analysis

* get_retry_events
* get_dlt_events

### Service Analysis

* get_events_by_service
* get_events_by_stage

### Performance Analysis

* get_processing_duration

Without ProcessLog data, these tools cannot function.

---

# 15. Best Practices

### Log Meaningful Events

Avoid logging unnecessary noise.

Each entry should represent a meaningful operational event.

---

### Always Include Business Identifiers

Include:

```text
jobId

recordId

memberId
```

whenever available.

---

### Use Consistent Status Values

Use standardized status values across services.

---

### Capture Failure Context

Always populate:

```text
errorType

errorMessage
```

for failures.

---

### Maintain Correlation

Preserve:

```text
correlationId

traceId
```

across service boundaries.

---

# 16. Summary

The ProcessLog model is the observability foundation of the TraceMind AI platform.

It provides a structured, searchable, and AI-friendly representation of operational events across all services.

By standardizing logging and maintaining consistent business identifiers, the platform enables complete traceability, efficient Splunk investigations, and intelligent MCP-driven production support workflows.

ProcessLog serves as the bridge between application execution and AI-assisted operational analysis.
