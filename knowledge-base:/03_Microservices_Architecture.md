# 03_Microservices_Architecture.md

# TraceMind AI - Microservices Architecture

## 1. Purpose

This document describes the microservices architecture of the TraceMind AI platform.

It provides an overview of each service, its responsibilities, interactions, data ownership, Kafka integration, and operational role within the system.

This document is intended for:

* Developers
* Support Engineers
* Architects
* Operations Teams

The goal is to establish clear service boundaries and simplify troubleshooting across the platform.

---

# 2. Architectural Overview

TraceMind AI follows a distributed microservices architecture built around asynchronous event-driven communication.

Each service is responsible for a specific business capability and communicates through Kafka events.

Benefits of this architecture include:

* Loose coupling
* Independent deployment
* Horizontal scalability
* Fault isolation
* Improved maintainability
* Clear ownership boundaries

---

# 3. Service Landscape

The platform currently consists of the following services:

```text
file-upload-service

pre-processor-service

email-service

print-service

archival-service

mcp-server
```

Supporting shared modules:

```text
common-lib
```

---

# 4. Service Interaction Diagram

```text
                CSV Upload
                     │
                     ▼
        file-upload-service
                     │
                     ▼
              record-created
                     │
                     ▼
         pre-processor-service
               │         │
               │         │
               ▼         ▼
        email-request   print-request
               │         │
               ▼         ▼
        email-service  print-service
               │         │
               └────┬────┘
                    ▼
            archival-request
                    │
                    ▼
            archival-service

------------------------------------------------

Claude Desktop
       │
       ▼
    MCP Server
       │
       ▼
     Splunk
```

---

# 5. Shared Library

## common-lib

### Purpose

Provides reusable components shared across all services.

### Responsibilities

* Common DTOs
* Event models
* ProcessLog models
* Constants
* Utility classes
* Shared configurations

### Benefits

* Reduces duplication
* Maintains consistency
* Simplifies maintenance

### Operational Notes

Business services should rely on common-lib for shared contracts rather than duplicating event definitions.

---

# 6. File Upload Service

## Service Name

file-upload-service

## Purpose

Acts as the entry point into the platform.

Responsible for receiving uploaded CSV files and initiating processing.

---

## Responsibilities

* Accept file uploads
* Validate uploaded files
* Generate job identifiers
* Parse CSV content
* Create records
* Persist job information
* Publish Kafka events

---

## Inputs

```text
CSV File
```

---

## Outputs

```text
record-created
```

---

## Database Ownership

Owns creation and maintenance of:

* Job records
* Record records

---

## Typical Support Questions

* Was the file uploaded successfully?
* Was a job created?
* Were records generated?
* Did CSV parsing fail?

---

## Failure Scenarios

* Invalid file format
* Empty file
* Parsing failures
* Database failures

---

# 7. Pre-Processor Service

## Service Name

pre-processor-service

## Purpose

Evaluates newly created records and determines the next processing path.

Acts as the orchestration layer within the business workflow.

---

## Responsibilities

* Consume record-created events
* Apply business validations
* Determine processing strategy
* Route records to downstream services
* Emit operational logs

---

## Consumes

```text
record-created
```

---

## Produces

```text
email-request

print-request
```

---

## Typical Support Questions

* Why was a record rejected?
* Why was email selected?
* Why was print selected?
* Did validation fail?

---

## Failure Scenarios

* Validation errors
* Business rule failures
* Event processing failures

---

# 8. Email Service

## Service Name

email-service

## Purpose

Processes email-related requests and delivers outbound communication.

---

## Responsibilities

* Consume email requests
* Generate email content
* Deliver emails
* Track delivery status
* Publish archival requests

---

## Consumes

```text
email-request
```

---

## Produces

```text
archival-request
```

---

## Typical Support Questions

* Was the email sent?
* Why did email delivery fail?
* Were retries attempted?
* Was the message sent to DLT?

---

## Failure Scenarios

* SMTP failures
* Template generation errors
* Network connectivity issues
* Provider downtime

---

# 9. Print Service

## Service Name

print-service

## Purpose

Processes records requiring print-based communication.

---

## Responsibilities

* Consume print requests
* Generate print artifacts
* Track print status
* Publish archival requests

---

## Consumes

```text
print-request
```

---

## Produces

```text
archival-request
```

---

## Typical Support Questions

* Was the print request generated?
* Why did print processing fail?
* Was the document created?

---

## Failure Scenarios

* Document generation failures
* Storage failures
* Processing exceptions

---

# 10. Archival Service

## Service Name

archival-service

## Purpose

Finalizes record processing and archives completed artifacts.

This service represents the final stage of the business workflow.

---

## Responsibilities

* Consume archival requests
* Archive processing artifacts
* Update record status
* Complete processing lifecycle

---

## Consumes

```text
archival-request
```

---

## Produces

No downstream business events.

---

## Typical Support Questions

* Was the record archived?
* Why was archival unsuccessful?
* Why is the record still processing?

---

## Failure Scenarios

* Storage failures
* Database failures
* Archival repository issues

---

# 11. MCP Server

## Service Name

mcp-server

## Purpose

Provides AI-powered production support capabilities through Model Context Protocol (MCP).

This service exposes operational investigation tools to Claude Desktop.

---

## Responsibilities

* Register MCP tools
* Execute Splunk queries
* Retrieve operational facts
* Support production investigations
* Enable natural language troubleshooting

---

## Integrated Systems

```text
Splunk
```

Future:

```text
PostgreSQL + pgvector
```

---

## Current MCP Tools

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

---

## Typical Support Questions

* Why did JOB-123 fail?
* Show timeline for REC-100
* Show retry events
* Which service failed?
* Show DLT activity

---

# 12. Service Communication Pattern

The platform follows asynchronous event-driven communication.

Services do not directly invoke one another.

Communication occurs through Kafka topics.

Benefits:

* Reduced coupling
* Independent scaling
* Improved resiliency
* Better fault tolerance

This architecture enables services to evolve independently while maintaining a consistent business workflow.

---

# 13. Service Ownership Principles

Each service owns:

* Its business capability
* Its processing logic
* Its emitted events
* Its operational logging

Services should not contain business logic belonging to another service.

This separation ensures maintainability and operational clarity.

---

# 14. Observability Model

Every service emits structured ProcessLog events.

Common fields include:

```text
timestamp
service
stage
action
status
jobId
recordId
memberId
correlationId
traceId
message
errorType
errorMessage
retryCount
```

Logs are written to:

```text
process.log
```

and monitored by Splunk.

This creates a unified operational view across all services.

---

# 15. Support Investigation Strategy

Support investigations typically follow one of three paths.

### Job Investigation

Start with:

```text
jobId
```

Goal:

Understand the complete file processing journey.

---

### Record Investigation

Start with:

```text
recordId
```

Goal:

Understand the lifecycle of a specific record.

---

### Service Investigation

Start with:

```text
service name
```

Goal:

Identify operational issues affecting a service.

---

# 16. Summary

The TraceMind AI platform is composed of independently deployable microservices connected through Kafka-based event-driven communication.

Each service owns a specific business capability and emits structured operational logs for traceability.

The MCP Server provides an AI-powered support layer that enables natural language access to operational insights while Splunk remains the authoritative source of production facts.

This architecture provides scalability, maintainability, fault isolation, and strong supportability across the platform.
