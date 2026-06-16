# 02_End_To_End_Business_Flow.md

# TraceMind AI - End-to-End Business Flow

## 1. Purpose

This document describes the complete business processing lifecycle within the TraceMind AI sample platform.

It explains how a file is processed from initial upload through downstream services until final archival and completion.

The document serves as a reference for:

* Support Engineers
* Developers
* Architects
* Production Operations Teams

Understanding this flow is critical when investigating job failures, record processing issues, retry behavior, and service-level incidents.

---

## 2. Business Overview

The platform processes bulk member data provided through CSV files.

Each uploaded file becomes a Job.

Each row within the CSV becomes an individual Record.

Records are processed independently through multiple downstream services using asynchronous Kafka messaging.

This architecture enables:

* Horizontal scalability
* Fault isolation
* Independent service ownership
* Event-driven processing
* Retry handling

---

## 3. Core Business Entities

### Job

A Job represents a single uploaded CSV file.

A Job acts as the parent entity for all records contained within the file.

Example:

```text
jobId = JOB-1001
```

Responsibilities:

* Track overall processing progress
* Aggregate record status
* Determine final completion state
* Support operational visibility

---

### Record

A Record represents an individual row within a CSV file.

Each record is processed independently.

Example:

```text
recordId = REC-2001
memberId = MEM-5001
```

Responsibilities:

* Member-level processing
* Email generation
* Print generation
* Archival processing

---

### Member

A Member represents the business entity being processed.

Example:

```text
memberId = MEM-5001
```

Member identifiers are included throughout the processing pipeline to simplify tracing and support investigations.

---

## 4. High-Level Processing Flow

```text
CSV Upload
    │
    ▼
File Upload Service
    │
    ▼
Create Job
    │
    ▼
Create Records
    │
    ▼
record-created Topic
    │
    ▼
Pre-Processor Service
    │
    ├──────────────► email-request
    │
    └──────────────► print-request
                          │
                          ▼
                Email / Print Services
                          │
                          ▼
                  archival-request
                          │
                          ▼
                 Archival Service
                          │
                          ▼
                 Record Completed
                          │
                          ▼
                   Job Completed
```

---

## 5. Detailed Processing Stages

### Stage 1 - File Upload

Responsible Service:

* file-upload-service

Input:

* CSV file

Activities:

* Validate file
* Generate jobId
* Persist job information
* Parse CSV rows

Outputs:

* Job created
* Records created
* Kafka events published

Success Result:

```text
Job Created
Records Generated
```

Potential Failures:

* Invalid file
* Empty file
* Parsing errors
* Database failures

---

### Stage 2 - Record Creation

Responsible Service:

* file-upload-service

Activities:

* Create individual records
* Assign recordId
* Associate memberId
* Store records in database

Output Event:

```text
record-created
```

Each record produces one Kafka event.

This event initiates downstream processing.

---

### Stage 3 - Pre-Processing

Responsible Service:

* pre-processor-service

Input Topic:

```text
record-created
```

Activities:

* Validate business rules
* Determine processing path
* Enrich processing metadata

Outputs:

```text
email-request
```

or

```text
print-request
```

The service determines the appropriate downstream action.

Potential Failures:

* Business validation failures
* Data quality issues
* Processing exceptions

---

### Stage 4A - Email Processing

Responsible Service:

* email-service

Input Topic:

```text
email-request
```

Activities:

* Generate email payload
* Perform email delivery
* Update processing status

Success Result:

```text
Email Sent
```

Output Event:

```text
archival-request
```

Potential Failures:

* SMTP connectivity issues
* Template generation failures
* Email provider failures

---

### Stage 4B - Print Processing

Responsible Service:

* print-service

Input Topic:

```text
print-request
```

Activities:

* Generate print document
* Process print request
* Update processing status

Success Result:

```text
Print Generated
```

Output Event:

```text
archival-request
```

Potential Failures:

* Document generation errors
* Print processing failures
* External dependency failures

---

### Stage 5 - Archival

Responsible Service:

* archival-service

Input Topic:

```text
archival-request
```

Activities:

* Archive processed artifacts
* Update record status
* Mark processing completion

Success Result:

```text
Archived Successfully
```

Potential Failures:

* Storage failures
* Archival repository issues
* Persistence errors

---

## 6. Status Lifecycle

### Record Lifecycle

```text
CREATED
    │
    ▼
PROCESSING
    │
    ▼
EMAIL_SENT
or
PRINT_COMPLETED
    │
    ▼
ARCHIVED
    │
    ▼
COMPLETED
```

Failure states may occur at any stage.

---

### Job Lifecycle

```text
CREATED
    │
    ▼
IN_PROGRESS
    │
    ▼
PARTIALLY_COMPLETED
or
COMPLETED
or
FAILED
```

Job status is determined by the aggregate state of all associated records.

---

## 7. Kafka Event Flow

The platform uses asynchronous event-driven communication.

Primary topics:

### record-created

Produced By:

* file-upload-service

Consumed By:

* pre-processor-service

---

### email-request

Produced By:

* pre-processor-service

Consumed By:

* email-service

---

### print-request

Produced By:

* pre-processor-service

Consumed By:

* print-service

---

### archival-request

Produced By:

* email-service
* print-service

Consumed By:

* archival-service

---

## 8. Retry and Failure Handling

The platform distinguishes between business failures and technical failures.

### Business Failures

Examples:

* Invalid member data
* Missing mandatory fields
* Business rule violations

Behavior:

* No retry
* No DLT

Reason:

The failure cannot be resolved through repeated execution.

---

### Technical Failures

Examples:

* Database unavailable
* SMTP unavailable
* Kafka connectivity issues
* Network interruptions

Behavior:

```text
Retry Attempt 1
      │
Retry Attempt 2
      │
Retry Attempt 3
      │
      ▼
DLT
```

Failed messages are moved to the Dead Letter Topic after retry exhaustion.

---

## 9. Traceability Model

The platform is designed for end-to-end traceability.

Primary identifiers:

### jobId

Used to trace an entire file processing journey.

Example:

```text
JOB-1001
```

---

### recordId

Used to trace a single record.

Example:

```text
REC-2001
```

---

### memberId

Used to trace business-level processing.

Example:

```text
MEM-5001
```

These identifiers appear in:

* Database records
* Kafka events
* ProcessLog entries
* Splunk searches
* MCP investigations

---

## 10. Support Investigation Examples

### Example 1

Question:

```text
What happened to JOB-1001?
```

Investigation:

* Retrieve job timeline
* Review processing stages
* Identify failures or retries

---

### Example 2

Question:

```text
Why did REC-2001 fail?
```

Investigation:

* Retrieve record timeline
* Identify failed service
* Check retry attempts
* Check DLT events

---

### Example 3

Question:

```text
Why was an email not delivered?
```

Investigation:

* Review email-service events
* Check retry history
* Review DLT records
* Consult email runbook

---

## 11. Summary

The TraceMind AI business flow is built around event-driven processing using Kafka and independently deployable microservices.

Each uploaded file becomes a Job.

Each row becomes a Record.

Records progress through validation, downstream processing, and archival stages while emitting structured ProcessLog events that provide complete operational traceability.

This processing model forms the foundation for Splunk investigations, MCP tool analysis, and AI-assisted production support.
