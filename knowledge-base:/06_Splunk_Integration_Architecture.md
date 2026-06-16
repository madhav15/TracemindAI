# 06_Splunk_Integration_Architecture.md

# TraceMind AI - Splunk Integration Architecture

## 1. Purpose

This document describes how Splunk is integrated into the TraceMind AI platform and how it serves as the authoritative source of operational facts.

It explains:

* Splunk's role within the platform
* Log ingestion architecture
* Data flow
* Search capabilities
* MCP integration
* Operational investigation workflows
* Design decisions

This document serves as the authoritative reference for all Splunk-related architecture and operational support activities.

---

# 2. Overview

TraceMind AI relies on Splunk as the centralized operational observability platform.

All business services emit structured ProcessLog events that are indexed by Splunk and later retrieved through MCP tools.

Splunk enables support teams and AI-powered investigations to answer questions such as:

* What happened to JOB-1001?
* Which service failed?
* Which records entered DLT?
* How many retries occurred?
* What was the processing duration?
* When did the failure occur?

Splunk is the single source of truth for operational system behavior.

---

# 3. Architectural Role of Splunk

Within TraceMind AI, Splunk is responsible for:

### Operational Fact Storage

Stores indexed ProcessLog events.

---

### Search and Investigation

Provides powerful search capabilities for production support investigations.

---

### Timeline Reconstruction

Allows reconstruction of complete job and record lifecycles.

---

### Failure Analysis

Supports identification of failures, retries, and DLT events.

---

### AI Fact Retrieval

Provides factual operational data consumed by MCP tools.

---

Splunk answers:

```text
WHAT happened?
```

It does not answer:

```text
WHY did it happen?

WHAT should be done next?
```

Those questions belong to the Knowledge Base layer.

---

# 4. High-Level Architecture

```text
Microservices
       │
       ▼
 Structured ProcessLog
       │
       ▼
    process.log
       │
       ▼
Splunk File Monitor
       │
       ▼
   Splunk Index
       │
       ▼
    MCP Server
       │
       ▼
 Claude Desktop
```

The architecture intentionally separates log generation from log indexing.

---

# 5. Log Ingestion Strategy

## Design Decision

Applications do not push data directly into Splunk.

Instead, applications write structured JSON logs to a local log file.

Splunk monitors the file and ingests new events automatically.

---

## Benefits

### Reduced Application Complexity

Services remain unaware of Splunk APIs.

---

### Improved Reliability

Application execution is not dependent on Splunk availability.

---

### Simpler Architecture

No SDKs, agents, or additional integrations are required within business services.

---

### Loose Coupling

Application services remain independent from observability tooling.

---

# 6. ProcessLog Generation

Every business service emits ProcessLog events.

Examples:

```text
Job Created

Record Created

Event Published

Event Consumed

Email Sent

Processing Failed

Retry Attempted

Moved To DLT
```

Each event is serialized as a single JSON document.

Example:

```json
{
  "service":"email-service",
  "status":"SUCCESS",
  "jobId":"JOB-1001",
  "recordId":"REC-2001"
}
```

These events become searchable operational facts.

---

# 7. Log Storage

All services write events to:

```text
process.log
```

This file acts as the ingestion source for Splunk.

Example:

```text
logs/process.log
```

The exact path may vary by deployment environment.

---

# 8. Splunk File Monitoring

Splunk continuously monitors the configured log file.

Whenever new entries appear:

```text
ProcessLog Event
       │
       ▼
process.log
       │
       ▼
Splunk Monitor
       │
       ▼
Indexing
```

The event becomes available for search almost immediately.

---

# 9. Splunk Index Design

## Index Name

```text
tracemind
```

All ProcessLog events are indexed into this location.

---

## Purpose

Provides centralized operational visibility for:

* Jobs
* Records
* Services
* Failures
* Retries
* DLT events

---

## Search Scope

All MCP investigations operate against this index.

---

# 10. Searchable Fields

The following fields are indexed and searchable.

### Business Identifiers

```text
jobId

recordId

memberId
```

---

### Service Information

```text
service

stage

action
```

---

### Processing State

```text
status

retryCount
```

---

### Failure Information

```text
errorType

errorMessage
```

---

### Traceability Information

```text
correlationId

traceId
```

---

### Time Information

```text
timestamp
```

These fields allow powerful operational investigations.

---

# 11. Splunk Search Capabilities

Typical searches include:

### Job Investigation

```text
jobId=JOB-1001
```

Purpose:

Reconstruct complete job lifecycle.

---

### Record Investigation

```text
recordId=REC-2001
```

Purpose:

Track a specific record.

---

### Service Investigation

```text
service=email-service
```

Purpose:

Analyze service activity.

---

### Retry Investigation

```text
status=RETRY
```

Purpose:

Identify technical failures.

---

### DLT Investigation

```text
status=DLT
```

Purpose:

Identify messages that exhausted retries.

---

# 12. MCP Integration Architecture

The MCP Server acts as the integration layer between Claude Desktop and Splunk.

```text
Claude Desktop
       │
       ▼
MCP Tool
       │
       ▼
Query Builder
       │
       ▼
Splunk Client
       │
       ▼
Splunk Search API
       │
       ▼
Search Results
       │
       ▼
Claude Response
```

This architecture isolates Splunk-specific logic within the MCP layer.

---

# 13. Splunk Integration Components

## SplunkProperties

Purpose:

Stores Splunk configuration.

Responsibilities:

* Host configuration
* Authentication settings
* Connection settings

---

## SplunkClient

Purpose:

Executes searches against Splunk.

Responsibilities:

* Authentication
* Search execution
* Response handling

---

## SplunkSearchRequest

Purpose:

Represents a search request.

Responsibilities:

* Query transport
* Request encapsulation

---

## SplunkSearchResponse

Purpose:

Represents search results returned from Splunk.

Responsibilities:

* Result transport
* Data encapsulation

---

## SplunkQueryBuilder

Purpose:

Constructs SPL queries.

Responsibilities:

* Query generation
* Query standardization
* Search optimization

---

# 14. MCP Tools Using Splunk

The following MCP tools retrieve operational facts directly from Splunk.

### Job Analysis

```text
get_job_timeline

get_job_summary
```

---

### Record Analysis

```text
get_record_timeline
```

---

### Failure Analysis

```text
get_failed_jobs

get_failed_emails
```

---

### Retry Analysis

```text
get_retry_events

get_dlt_events
```

---

### Service Analysis

```text
get_events_by_service

get_events_by_stage
```

---

### Performance Analysis

```text
get_processing_duration
```

---

# 15. Why Logs Are Not Embedded

A key architectural decision is that operational logs are never stored within the vector knowledge repository.

The following data is not embedded:

```text
ProcessLog Events

Kafka Events

Job History

Record History

Retry Events

DLT Events
```

---

## Reasons

### High Volume

Operational logs grow continuously.

---

### Rapid Change

Production data changes constantly.

---

### Poor Semantic Value

Operational facts are best retrieved through precise searches.

---

### Data Freshness

Splunk always contains the most current state.

---

### Clear Separation of Concerns

Splunk stores operational truth.

Knowledge storage contains documentation and guidance.

---

# 16. Splunk vs Knowledge Base

| Capability          | Splunk  | Knowledge Base |
| ------------------- | ------- | -------------- |
| Job Timeline        | Yes     | No             |
| Record Timeline     | Yes     | No             |
| Retry Events        | Yes     | No             |
| DLT Events          | Yes     | No             |
| Architecture Docs   | No      | Yes            |
| SOPs                | No      | Yes            |
| Runbooks            | No      | Yes            |
| Policies            | No      | Yes            |
| Root Cause Guidance | Limited | Yes            |

---

## Principle

Splunk answers:

```text
WHAT happened?
```

Knowledge Base answers:

```text
WHY did it happen?

WHAT should be done next?
```

Claude combines both.

---

# 17. Example Investigation Workflow

User Question:

```text
Why did JOB-1001 fail?
```

Step 1:

Claude invokes:

```text
get_job_summary
```

---

Step 2:

Claude invokes:

```text
get_retry_events
```

---

Step 3:

Claude invokes:

```text
get_dlt_events
```

---

Step 4:

Claude gathers operational facts.

---

Step 5:

Claude retrieves relevant knowledge documents.

---

Step 6:

Claude synthesizes a complete answer.

---

# 18. Design Principles

### Splunk Is the Source of Truth

All operational facts originate from Splunk.

---

### Structured Logging First

Every service emits consistent ProcessLog events.

---

### Loose Coupling

Applications remain independent of Splunk APIs.

---

### Search Over Storage

Operational investigations rely on Splunk search rather than duplicated storage.

---

### AI-Augmented Support

Splunk provides facts.

AI provides interpretation.

---

# 19. Summary

Splunk serves as the operational observability foundation of the TraceMind AI platform.

All services emit structured ProcessLog events that are indexed into the tracemind Splunk index and become searchable operational facts.

The MCP Server retrieves these facts through Splunk searches and exposes them to Claude Desktop through MCP tools.

By keeping operational data in Splunk and knowledge in a separate repository, TraceMind AI maintains a clear separation between factual system behavior and contextual operational guidance, enabling accurate and explainable AI-assisted production support.
