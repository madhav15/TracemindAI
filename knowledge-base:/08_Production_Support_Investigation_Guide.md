# 08_Production_Support_Investigation_Guide.md

# TraceMind AI - Production Support Investigation Guide

## 1. Purpose

This document provides a standardized approach for investigating production issues within the TraceMind AI platform.

The goal is to ensure consistent, repeatable, and efficient troubleshooting across all environments.

This guide is intended for:

* Production Support Engineers
* Application Support Teams
* Developers
* Technical Leads
* Architects

It describes how to investigate failures using business identifiers, Splunk searches, MCP tools, and operational knowledge.

---

# 2. Investigation Philosophy

All investigations should follow a fact-first approach.

Support engineers should first determine:

```text
What happened?
```

before attempting to determine:

```text
Why it happened?
```

or

```text
What should be done next?
```

Operational facts are retrieved from Splunk.

Contextual understanding is retrieved from the Knowledge Base.

---

# 3. Primary Investigation Identifiers

The platform is designed around business identifiers rather than database-generated IDs.

Always start investigations using one of the following identifiers.

### jobId

Used for file-level investigations.

Example:

```text
JOB-1001
```

---

### recordId

Used for individual record investigations.

Example:

```text
REC-2001
```

---

### memberId

Used for business-level investigations.

Example:

```text
MEM-5001
```

---

## Recommendation

Whenever possible:

```text
Start with jobId
```

because it provides the broadest operational view.

---

# 4. Standard Investigation Workflow

Every production issue should follow the same sequence.

```text
Identify Entity
       │
       ▼
Retrieve Timeline
       │
       ▼
Identify Failure Point
       │
       ▼
Review Retry Events
       │
       ▼
Review DLT Events
       │
       ▼
Determine Root Cause
       │
       ▼
Consult Runbook
       │
       ▼
Recommend Resolution
```

This process minimizes guesswork and improves investigation consistency.

---

# 5. Job Investigation

## Scenario

User reports:

```text
JOB-1001 failed
```

---

## Objective

Determine where processing stopped and why.

---

## Recommended MCP Tools

### Step 1

```text
get_job_summary
```

Purpose:

Obtain a high-level operational overview.

---

### Step 2

```text
get_job_timeline
```

Purpose:

View complete processing history.

---

### Step 3

```text
get_retry_events
```

Purpose:

Identify technical failures and retries.

---

### Step 4

```text
get_dlt_events
```

Purpose:

Determine whether retry exhaustion occurred.

---

## Typical Outcome

Identify:

* Failed service
* Failed stage
* Retry activity
* DLT activity
* Final job status

---

# 6. Record Investigation

## Scenario

User reports:

```text
REC-2001 failed
```

---

## Objective

Determine the lifecycle of a specific record.

---

## Recommended MCP Tool

```text
get_record_timeline
```

---

## Investigation Questions

* Was the record created?
* Was the event published?
* Which service processed it?
* Which stage failed?
* Was processing completed?

---

# 7. Service Investigation

## Scenario

User reports:

```text
Email service appears to be failing.
```

---

## Recommended MCP Tool

```text
get_events_by_service
```

---

## Investigation Goals

Determine:

* Volume of failures
* Failure patterns
* Error types
* Retry frequency
* DLT activity

---

## Typical Questions

* Are failures isolated?
* Are failures widespread?
* Is a downstream dependency unavailable?

---

# 8. Stage Investigation

## Scenario

User reports:

```text
Processing is stuck during archival.
```

---

## Recommended MCP Tool

```text
get_events_by_stage
```

---

## Objective

Analyze all activity related to a specific processing stage.

---

## Typical Findings

* Processing bottlenecks
* Increased failures
* Delayed execution
* Infrastructure issues

---

# 9. Retry Investigation

## Scenario

User reports:

```text
The job eventually succeeded but experienced delays.
```

---

## Recommended MCP Tool

```text
get_retry_events
```

---

## Investigation Questions

* How many retries occurred?
* Which service retried?
* What triggered retries?
* Did processing recover?

---

## Interpretation

Retries often indicate transient technical issues.

Examples:

* SMTP outage
* Database timeout
* Kafka connectivity issue
* Temporary network problem

---

# 10. DLT Investigation

## Scenario

User reports:

```text
Processing never completed.
```

---

## Recommended MCP Tool

```text
get_dlt_events
```

---

## Objective

Determine whether messages exhausted retry attempts.

---

## Investigation Questions

* Which event entered DLT?
* Which service generated the DLT event?
* What error caused failure?
* Can processing be retried manually?

---

## Typical Causes

* Persistent infrastructure failure
* Invalid payload
* Dependency outage
* Configuration issue

---

# 11. Failed Email Investigation

## Scenario

User reports:

```text
Customer did not receive email.
```

---

## Recommended MCP Tool

```text
get_failed_emails
```

---

## Investigation Process

### Verify Email Request

Confirm email-request was published.

---

### Verify Consumption

Confirm email-service received the event.

---

### Review Failure Events

Identify:

```text
errorType

errorMessage
```

---

### Review Retry History

Determine whether retries occurred.

---

### Review DLT Activity

Determine whether processing exhausted retries.

---

## Common Root Causes

* SMTP unavailable
* Email provider outage
* Invalid recipient data
* Network interruption

---

# 12. Processing Duration Investigation

## Scenario

User reports:

```text
Processing is slow.
```

---

## Recommended MCP Tool

```text
get_processing_duration
```

---

## Investigation Goals

Determine:

* Total processing duration
* Service delays
* Processing bottlenecks
* Stage-specific latency

---

## Typical Findings

* Email delays
* Archival delays
* Kafka backlog
* Infrastructure degradation

---

# 13. Root Cause Analysis Framework

After gathering facts, determine the category of failure.

---

## Business Failure

Examples:

```text
Invalid Member Data

Missing Mandatory Fields

Business Rule Violation
```

Characteristics:

* No retries
* No DLT
* Immediate failure

---

## Technical Failure

Examples:

```text
SMTP Failure

Database Failure

Kafka Failure

Network Timeout
```

Characteristics:

* Retries present
* Possible DLT activity
* Infrastructure dependency involved

---

# 14. Escalation Guidelines

Escalate when:

### Multiple Jobs Affected

Potential platform-wide issue.

---

### High DLT Volume

Potential systemic failure.

---

### Infrastructure Outage

Requires platform support.

---

### Data Corruption

Requires engineering investigation.

---

### Unknown Failure Pattern

Requires developer involvement.

---

# 15. Common Investigation Mistakes

### Jumping to Conclusions

Always gather facts first.

---

### Ignoring Retry History

Retries often reveal the true failure source.

---

### Ignoring DLT Events

DLT events frequently contain the final failure reason.

---

### Focusing Only on Error Messages

Always review the complete timeline.

---

### Investigating Without Business Identifiers

Start with:

```text
jobId

recordId

memberId
```

whenever possible.

---

# 16. MCP Investigation Playbooks

## Job Failure

```text
get_job_summary
        │
get_job_timeline
        │
get_retry_events
        │
get_dlt_events
```

---

## Record Failure

```text
get_record_timeline
```

---

## Email Failure

```text
get_failed_emails
        │
get_retry_events
        │
get_dlt_events
```

---

## Service Failure

```text
get_events_by_service
```

---

## Performance Issue

```text
get_processing_duration
```

---

# 17. AI-Assisted Investigation Model

TraceMind AI combines operational facts with organizational knowledge.

Investigation workflow:

```text
Splunk
      │
      ▼
Operational Facts
      │
      ▼
Knowledge Base
      │
      ▼
Runbooks
Policies
FAQs
      │
      ▼
Claude Analysis
      │
      ▼
Final Recommendation
```

This approach reduces investigation time and improves consistency.

---

# 18. Summary

Effective production support begins with understanding what happened before determining why it happened.

The TraceMind AI platform provides structured MCP tools, Splunk-based operational facts, and knowledge-driven guidance to support rapid and consistent investigations.

By following the workflows described in this guide, support engineers can systematically diagnose failures, identify root causes, and recommend appropriate corrective actions while maintaining a repeatable and auditable investigation process.
