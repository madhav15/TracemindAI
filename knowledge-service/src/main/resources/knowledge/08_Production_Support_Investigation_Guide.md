# Production Support and Investigation Guide

Production support for the TraceMind platform follows a structured investigation methodology that progresses from high-level overview to specific root cause. The methodology is designed to be fast, repeatable, and effective regardless of which service or stage the issue originates from. The primary tools used during investigation are the MCP server's query tools, which provide direct access to the platform's Splunk-indexed event stream. This guide walks through the typical investigation flow and the tools to use at each step.

## Step 1: Identify the Scope

The first step in any investigation is to determine whether the issue is isolated to a single job, a single record, or systemic. This scoping determines which investigation path to follow. If the user reports "job JOB-123 is broken," the scope is a single job. If the user reports "this one member didn't get their email," the scope is a single record. If the user reports "emails are failing across the board," the scope is systemic.

For a single job, use the get_job_timeline tool to see the full lifecycle. For a single record, use the get_record_timeline tool to see how that record was processed across services. For a systemic issue, use the get_failed_jobs, get_retry_events, or get_dlt_events tools to see system-wide patterns. The choice of tool at this step sets the direction for the rest of the investigation.

## Step 2: Locate the Failure Point

Once the scope is identified, the next step is to locate the first failure point in the event stream. For a single job or record, the get_*_timeline tool returns events in chronological order, so the first event with status FAILED is the most likely root cause. Examine that event's error message: it should explain why the operation failed.

For a systemic issue, the get_failed_jobs tool returns all FAILED events across the system. Look for common error messages: if many failures share the same error, the issue is likely a shared dependency (SMTP server, print vendor, database) that is having a problem. If the error messages are diverse, the issue may be a recent code change that introduced a new failure mode.

## Step 3: Determine the Impact

After locating the failure point, determine how many records or jobs are affected. The get_retry_events tool shows events that are being retried — a high retry count on a particular service indicates that the service is having trouble and is recovering through retries. The get_dlt_events tool shows events that have exhausted retries and require manual intervention. The size of the DLT backlog is a direct measure of the operational impact.

For email-specific issues, use the get_failed_emails tool to see the failures scoped to the email-service. This is useful when the user reports an email problem and you want to filter out other types of failures. The tool returns the same structure as get_failed_jobs but with the additional service filter applied.

## Step 4: Investigate the Root Cause

With the failure point and impact identified, the next step is to determine the root cause. The error message in the FAILED event is the starting point. Common error patterns and their likely causes include: SMTP timeout (mail server is slow or down), invalid email format (data quality issue in the source CSV), template rendering error (template file is missing or has a syntax error), print vendor 5xx (third-party API is having an outage), database connection error (database is overloaded or unreachable), and out of memory (JVM needs more heap).

For systemic issues, use the get_events_by_service and get_events_by_stage tools to see event volume distribution. A sudden spike in events at a particular service or stage indicates where the bottleneck is. The get_processing_duration tool can be used to see if processing times are degrading over time, which is often the leading indicator of an upcoming outage.

## Step 5: Take Action

Once the root cause is identified, take the appropriate action. For transient failures (network blip, temporary downstream unavailability), the platform's retry policy will handle the recovery automatically — no action is needed beyond monitoring the situation. For persistent failures, the action depends on the failure type: restart the affected service if it's hung, fix the data quality issue if it's a CSV problem, contact the third-party vendor if it's an API outage, or apply a code fix if it's a bug.

For DLT events, the action is manual intervention. Each DLT event includes the original payload and the error context, so an operator can examine the data, fix the underlying issue (or document it as a known-bad record), and either reprocess the event or discard it. The DLT should be reviewed daily by the operations team to ensure that the backlog does not grow unbounded.

## Step 6: Document and Prevent

After the issue is resolved, document the root cause and the remediation in the platform's incident log. This builds up an organizational knowledge base that makes future investigations faster. If the issue is likely to recur, consider adding a monitoring alert for the leading indicators (consumer lag spike, retry count spike, error rate spike) so that the operations team is notified before the issue becomes customer-visible.

For recurring issues, the knowledge-service's semantic search (once integrated with the MCP server) can be used to find relevant documentation and past incident reports. The combination of operational data and historical context makes the platform's support process both faster and more thorough as the organization's experience with the platform grows.

## Common Pitfalls

There are a few common pitfalls to avoid during investigation. First, don't jump to conclusions based on a single FAILED event — there may be earlier failures that were retried successfully. Second, don't ignore the retry count — a high retry count indicates systemic stress even if individual events eventually succeed. Third, don't dismiss DLT events as "edge cases" — they are records that the platform could not process and they represent direct business impact. Fourth, don't rely solely on Splunk — the services' own database tables (job, record, pre_processor_tracking, email_tracking, print_job, archival_job) contain authoritative state that can confirm or refute what the Splunk events suggest.
