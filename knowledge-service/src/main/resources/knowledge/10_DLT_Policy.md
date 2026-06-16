# DLT (Dead Letter Topic) Policy

The Dead Letter Topic (DLT) is the terminal destination for events that cannot be processed successfully after all retries have been exhausted. The DLT is the platform's last line of defense against data loss: it ensures that no event is silently dropped, and it provides operations with a clear, actionable list of records that require manual intervention. The DLT is not a failure — it is a deliberate, structured way to handle unrecoverable processing failures.

## What Goes to the DLT

An event is routed to the DLT when one of three conditions is met. First, the event has been retried the maximum number of times and is still failing. Second, the event has encountered a permanent failure (invalid data, missing required fields, authentication failure, 4xx HTTP response) that would not benefit from retry. Third, the event has been explicitly marked as poison by the consumer (for example, because deserialization failed and the event cannot be processed at all).

In all three cases, the original event payload is preserved unchanged in the DLT event. The DLT event is an envelope around the original event, with additional metadata: the source topic, the source service, the error message that triggered the DLT routing, the retry count, and the timestamp of the DLT routing. This metadata is what makes DLT events actionable: an operator can read the error message, understand what went wrong, and decide on the appropriate remediation.

## DLT Storage

The DLT has two storage destinations. First, the dlt_events table in the platform's PostgreSQL database. This is the authoritative store for DLT events: every event that is routed to the DLT is also written to this table, with a unique ID, the full payload, the error context, and the routing metadata. The table is indexed by jobId, recordId, and error type, enabling fast queries for analysis and reporting.

Second, Splunk. The DLT routing is logged as a ProcessLog entry with status DLT, and this entry flows to Splunk through the standard log shipping pipeline. The Splunk entry is what the MCP server's get_dlt_events tool queries, providing a way to see the DLT backlog from Claude Desktop. The two storage destinations serve different purposes: the database for authoritative state and audit, Splunk for operational visibility and analysis.

## Operational Workflow

The operations team reviews the DLT backlog daily. The review process starts with the get_dlt_events MCP tool, which returns the recent DLT events with their error messages. The team groups the events by error type: events with the same error type usually have the same root cause, and addressing the root cause may resolve many events at once. For each group, the team investigates the root cause and decides on one of three actions: reprocess, fix-and-reprocess, or discard.

Reprocess is used when the underlying issue has been resolved (for example, the SMTP server is back up) and the events can be safely retried. The team re-injects the events at the appropriate stage of the pipeline, and they flow through the normal processing. Fix-and-reprocess is used when the events have a data quality issue that can be corrected programmatically (for example, normalizing phone number formats). The team applies the fix to the events and reprocesses them. Discard is used when the events are known-bad data that should not be processed (for example, test data that was accidentally included in a production job). The team documents the discard decision in the incident log.

## DLT Backlog Management

The DLT backlog should be kept small. A growing backlog is a sign that the operations team is not keeping up with the daily review, which means that important records are not being processed. The platform's monitoring system alerts on DLT backlog size exceeding a threshold (typically 1000 events), triggering an escalation to the on-call engineer. The on-call engineer is responsible for triaging the backlog, identifying the root cause of the spike, and bringing the backlog size back below the threshold.

The DLT backlog size is also a leading indicator of platform health. A sudden spike in DLT events (for example, from 10 events per day to 500 events per day) indicates a systemic issue: a recent deployment may have introduced a bug, a downstream dependency may be having an outage, or a data source may have changed its format. The operations team should investigate any DLT spike immediately, rather than waiting for the daily review.

## Long-Term DLT Records

Some DLT events are intentionally not reprocessed. These are typically records that represent business decisions rather than technical failures: for example, a member who has opted out of communications, a record that fails validation because the member has been marked as deceased, or a record that has been flagged as duplicate. The operations team should document these cases in the dlt_events table with a clear explanation, and the platform's monitoring should exclude them from the backlog size alert to avoid noise.

For audit purposes, DLT records are retained for a long period (typically one year). This ensures that if a question arises about why a particular record was not processed, the team can find the DLT event and its associated error message. The audit retention policy is configured per environment: development has a shorter retention (30 days) to save storage, production has a longer retention (1 year) for compliance.

## DLT as a Signal

The DLT is not just a place to put failed events — it is also a signal source. The patterns in DLT events (which error types are most common, which services are most affected, which jobs are producing the most DLT events) are diagnostic information that helps the team identify systemic issues. The platform's DLT analysis tools aggregate this information and surface it in dashboards that the operations team reviews weekly. The dashboards highlight trends (for example, a slowly growing DLT rate over the past month) that may not be visible in day-to-day operations.

The knowledge-service's documentation includes a section on the DLT policy, and the MCP server's get_dlt_events tool can be used to query the current state. The combination of policy, documentation, tooling, and operational workflow is what makes the DLT a manageable, actionable part of the platform rather than a black hole for failed events.
