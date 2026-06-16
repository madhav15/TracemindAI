# ProcessLog Observability Model

The ProcessLog model is the standardized observability schema used by every service in the TraceMind platform. It defines the structure and content of every log entry that represents a processing event, ensuring that events from different services can be queried and correlated uniformly. The model is intentionally minimal: it captures only the fields that are universally relevant across all services, and it leaves service-specific details to be captured in the event payload or in additional structured log fields.

## Core Fields

Every ProcessLog entry contains six core fields. The jobId is the unique identifier of the business job that the event relates to, set at ingestion time and propagated through every stage of the pipeline. The recordId is the unique identifier of the individual member record, also set at ingestion and propagated forward. The service field is the name of the service that produced the log entry (file-upload-service, pre-processor-service, email-service, print-service, archival-service). The stage field is the name of the processing stage within that service (validation, transformation, delivery, archival). The status field is one of STARTED, IN_PROGRESS, SUCCESS, FAILED, RETRIED, or DLT. The timestamp is the ISO 8601 timestamp of when the event occurred.

In addition to the core fields, ProcessLog entries may include an errorMessage field (populated for FAILED and DLT statuses), a retryCount field (populated for RETRIED status), and a durationMs field (populated for completed events). The model is extensible: services can add their own custom fields for service-specific observability, but the core six fields are always present and always named consistently.

## Status Semantics

The ProcessLog model uses a closed set of status values, each with well-defined semantics. STARTED indicates the beginning of a processing operation — the service has received the event and is about to begin work. IN_PROGRESS indicates that the service is actively working on the event, with optional progress indicators. SUCCESS indicates that the operation completed successfully. FAILED indicates that the operation failed in a way that may be recoverable through retry. RETRIED indicates that a FAILED operation is being retried. DLT indicates that the operation has been routed to the Dead Letter Topic after retry exhaustion.

The status transitions are well-defined. A processing operation typically moves through STARTED → IN_PROGRESS → SUCCESS, or STARTED → IN_PROGRESS → FAILED → RETRIED → ... → DLT. The transitions are recorded in Splunk as separate log entries, so the full state machine for each processing operation can be reconstructed from the logs.

## Use in Splunk

ProcessLog entries are written to stdout in JSON format by each service. The platform's log shipper collects these entries and forwards them to Splunk, where they are indexed with the service name and timestamp as primary index fields. The jobId, recordId, stage, and status are extracted as additional indexed fields, enabling fast queries by these dimensions.

The standard Splunk queries used by the MCP server all rely on the ProcessLog model. The get_job_timeline query is `search index=tracemind jobId="X" | sort _time`, which returns all ProcessLog entries for the specified job in chronological order. The get_failed_jobs query is `search index=tracemind status="FAILED"`, which returns all FAILED entries across the system. The get_events_by_service query is `search index=tracemind | stats count by service`, which counts entries grouped by the service field. Every MCP tool maps to a Splunk query that uses one or more ProcessLog fields as the filter or aggregation dimension.

## Correlation Across Services

The most powerful feature of the ProcessLog model is correlation. Because every event includes the jobId and recordId, an operator can start with a single jobId and see every event related to that job across all services. The events are naturally ordered by timestamp, so the operator can see the full lifecycle of the job: ingestion at time T0, pre-processing at T1, email delivery attempts at T2 and T3, archival at T4. If any of these steps failed, the error message is in the same log entry, providing immediate root-cause visibility.

Record-level correlation works the same way. Starting with a single recordId, the operator can see every service that touched that record, every stage it passed through, and every status transition it underwent. This is invaluable for debugging individual record failures: "why didn't this specific member receive their email?" becomes a Splunk query rather than a code investigation.

## Why JSON

ProcessLog entries are written in JSON rather than free-form text for two reasons. First, JSON is machine-parseable: Splunk can extract fields from JSON automatically without requiring custom regex patterns, which makes the search queries simpler and more reliable. Second, JSON is structured: every entry has the same shape, so the queries are predictable and the results are consistent. A free-form log message might say "Job JOB-123 failed at stage X" or "Stage X failed for job JOB-123" or "Failure: JOB-123 at X" — and each variation would require a different regex to extract the fields. With JSON, the fields are always at the same path in the structure.

The platform's logging library handles the JSON serialization automatically. Developers only need to call `processLogger.logEvent(jobId, recordId, service, stage, status)` and the library takes care of building the JSON structure, adding the timestamp, and writing it to stdout. This minimizes the chance of inconsistent log formats across services.
