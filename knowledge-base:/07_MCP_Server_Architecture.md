# 07_MCP_Server_Architecture.md

# TraceMind AI - MCP Server Architecture

## 1. Purpose

This document describes the architecture of the Model Context Protocol (MCP) Server used within the TraceMind AI platform.

The MCP Server acts as the integration layer between Claude Desktop and enterprise operational systems.

Its primary responsibility is to expose production support capabilities as AI-consumable tools that can be invoked through natural language conversations.

This document serves as the authoritative reference for:

* MCP architecture
* Tool execution lifecycle
* Claude Desktop integration
* Splunk integration
* Tool design principles
* Future knowledge retrieval capabilities

---

# 2. Overview

TraceMind AI uses the Model Context Protocol (MCP) to allow Claude Desktop to interact with enterprise systems through structured tools.

Instead of directly accessing databases or observability platforms, Claude invokes MCP tools exposed by the MCP Server.

This architecture enables:

* Natural language production support
* Controlled system access
* Tool-based integrations
* Secure operational investigations
* Explainable AI interactions

---

# 3. High-Level Architecture

```text
User
   │
   ▼
Claude Desktop
   │
   ▼
MCP Protocol
   │
   ▼
Spring Boot MCP Server
   │
   ├────────────► Splunk
   │
   └────────────► Knowledge Repository
                  (Future)
```

The MCP Server acts as the gateway between AI interactions and enterprise systems.

---

# 4. Why MCP

Traditional AI integrations often require custom APIs, plugins, or application-specific connectors.

MCP provides a standardized mechanism for exposing enterprise capabilities as tools.

Benefits include:

### Standardized Integration

Common protocol between AI clients and enterprise services.

---

### Tool Discovery

Claude automatically discovers available tools.

---

### Natural Language Interaction

Users can interact using business language rather than technical queries.

---

### Extensibility

New tools can be added without changing Claude Desktop.

---

### Controlled Access

Only explicitly exposed capabilities are available to AI systems.

---

# 5. Current Deployment Model

The MCP Server is implemented as a Spring Boot application.

Communication occurs using:

```text
STDIO Transport
```

Flow:

```text
Claude Desktop
        │
        ▼
STDIO
        │
        ▼
Spring AI MCP Server
```

This deployment model is simple, reliable, and suitable for local development and demonstrations.

---

# 6. Tool Execution Lifecycle

A typical request follows the sequence below.

```text
User Question
       │
       ▼
Claude Analysis
       │
       ▼
Tool Selection
       │
       ▼
MCP Tool Invocation
       │
       ▼
Splunk Query
       │
       ▼
Search Results
       │
       ▼
Claude Response
```

The MCP Server is responsible only for retrieving facts.

Claude is responsible for synthesizing the final answer.

---

# 7. Tool Registration

During startup, the MCP Server registers all available tools.

Startup sequence:

```text
Server Start
      │
      ▼
Tool Registration
      │
      ▼
Claude Discovery
      │
      ▼
Tool Availability
```

Claude Desktop automatically discovers registered tools.

No manual configuration is required after registration.

---

# 8. Claude Desktop Integration

The MCP integration follows the standard MCP lifecycle.

### Initialization

```text
initialize
```

Used to establish communication.

---

### Tool Discovery

```text
tools/list
```

Used by Claude to retrieve available tools.

---

### Tool Invocation

```text
tools/call
```

Used to execute a selected tool.

---

### Result Processing

Claude receives the response and incorporates it into the conversation.

---

# 9. Current MCP Tool Inventory

The platform currently exposes the following production support tools.

---

## Job Investigation Tools

### get_job_timeline

Purpose:

Retrieve the chronological processing history of a job.

---

### get_job_summary

Purpose:

Provide a summarized operational view of a job.

---

## Record Investigation Tools

### get_record_timeline

Purpose:

Retrieve the processing history of a specific record.

---

## Failure Investigation Tools

### get_failed_jobs

Purpose:

Identify failed jobs.

---

### get_failed_emails

Purpose:

Identify failed email processing events.

---

## Retry Analysis Tools

### get_retry_events

Purpose:

Retrieve retry-related events.

---

### get_dlt_events

Purpose:

Retrieve Dead Letter Topic events.

---

## Service Analysis Tools

### get_events_by_service

Purpose:

Retrieve events associated with a specific service.

---

### get_events_by_stage

Purpose:

Retrieve events associated with a processing stage.

---

## Performance Analysis Tools

### get_processing_duration

Purpose:

Calculate processing duration metrics.

---

# 10. Internal Architecture

The MCP Server follows a simple architecture.

```text
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
Response
```

The architecture intentionally avoids unnecessary abstraction layers.

---

# 11. Core Components

## SplunkProperties

Stores Splunk configuration.

Responsibilities:

* Host configuration
* Authentication settings
* Connection properties

---

## SplunkClient

Executes searches against Splunk.

Responsibilities:

* Authentication
* Query execution
* Response retrieval

---

## SplunkSearchRequest

Represents a search request.

---

## SplunkSearchResponse

Represents search results.

---

## SplunkQueryBuilder

Generates SPL queries used by MCP tools.

---

# 12. Tool Design Principles

All MCP tools follow consistent design principles.

### Business Identifier Driven

Tools use:

```text
jobId

recordId

memberId
```

rather than database identifiers.

---

### Fact Retrieval Only

Tools retrieve operational facts.

They do not generate interpretations.

---

### Single Responsibility

Each tool performs one investigation task.

---

### Explainability

Results must be traceable to Splunk data.

---

### Predictable Output

Tools should return structured and consistent responses.

---

# 13. Future Knowledge Integration

Future versions of the MCP Server will integrate with PostgreSQL + pgvector.

Architecture:

```text
Claude Desktop
       │
       ▼
MCP Server
       │
       ├──────────► Splunk
       │
       └──────────► PostgreSQL pgvector
```

---

## Planned Knowledge Tool

### search_knowledge

Purpose:

Perform semantic search across enterprise knowledge documents.

Example queries:

* How does retry work?
* Explain DLT handling.
* What is the archival process?
* How does email-service operate?

---

# 14. Fact and Knowledge Separation

A key architectural principle is separation of facts and knowledge.

### Splunk

Provides:

```text
WHAT happened?
```

---

### Knowledge Repository

Provides:

```text
WHY it happened?

WHAT should be done next?
```

---

### Claude

Combines both sources into a single response.

---

# 15. Example Investigation Flow

User Question:

```text
Why did JOB-1001 fail?
```

Claude:

```text
get_job_summary
       │
get_retry_events
       │
get_dlt_events
```

Operational facts are retrieved.

Knowledge documents are consulted.

Claude synthesizes a complete response.

---

# 16. Design Principles

### Simplicity

Keep architecture easy to understand.

---

### Minimal Abstractions

Avoid unnecessary design patterns.

---

### Tool-Based Integration

Expose capabilities through focused tools.

---

### Enterprise Readiness

Support production investigation workflows.

---

### Extensibility

Allow future knowledge-search capabilities.

---

# 17. Summary

The MCP Server is the central integration layer of TraceMind AI.

It enables Claude Desktop to retrieve operational facts from enterprise systems through structured tools while maintaining clear separation between AI interactions and backend systems.

By exposing focused investigation capabilities through MCP, the platform transforms production support from manual log analysis into conversational, AI-assisted troubleshooting.
