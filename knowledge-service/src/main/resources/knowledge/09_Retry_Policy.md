# Retry Policy

The TraceMind platform implements a structured retry policy to handle transient failures gracefully. The policy is designed to maximize the chance of successful processing while bounding the cost of repeated failures. Every service in the platform applies the same retry semantics, ensuring consistent behavior across the pipeline. The retry policy is implemented in the common-lib module so that all services use the same retry logic and the same constants for retry counts and backoff intervals.

## Retry Triggers

A retry is triggered when a service encounters a failure that is classified as transient. Transient failures include network timeouts, temporary downstream service unavailability, rate limiting from external APIs, and database connection failures that resolve on the next attempt. The classification is performed by the service's exception handling code, which inspects the exception type and the response code (for HTTP failures) to determine whether the failure is transient or permanent.

Permanent failures (such as invalid data, missing required fields, authentication failures, or 4xx HTTP responses from external APIs) are not retried. They are immediately routed to the DLT, because retrying them would only waste resources and delay the eventual routing to the DLT. The distinction between transient and permanent is critical: retrying a permanent failure is a waste, and routing a transient failure to the DLT too early is a missed opportunity for recovery.

## Retry Count and Backoff

The platform uses exponential backoff for retries. The first retry occurs after a short delay (typically 1 second), the second retry occurs after a longer delay (typically 5 seconds), and the third retry occurs after an even longer delay (typically 30 seconds). The exact delays are configurable per service, but the exponential pattern is consistent. The platform's default maximum retry count is 3, meaning that an event is attempted up to 4 times total (1 initial attempt plus 3 retries) before being routed to the DLT.

The retry count is captured in the event envelope and in the ProcessLog entry. When a service publishes a retried event, the event's retryCount field is incremented, and the consumer uses this count to determine whether to process the event or to route it to the DLT. The retry count is also visible in Splunk, where it can be queried to identify services that are experiencing high retry rates.

## Retry Topic

Retried events are published to a dedicated retry topic rather than being republished to the original topic. The retry topic uses the same Kafka infrastructure as the other topics, but its events are scheduled for delayed delivery. The platform implements the delayed delivery using a scheduled task that holds the event in memory (or in a lightweight database) until the delay has elapsed, then republishes it to the original topic. This indirection allows the original topic to continue processing new events without being slowed down by retried events.

The retry topic also serves as a debugging surface. If an event is being retried multiple times, it will appear multiple times in the retry topic, and each appearance is logged to Splunk. This makes it easy to identify events that are "flapping" — succeeding once, then failing, then succeeding again, then failing again. Flapping events are usually a sign of an unstable downstream dependency, and the flapping pattern is a strong signal that the dependency needs investigation.

## Idempotency

A prerequisite for safe retry is idempotency: the consumer must be able to process the same event multiple times without producing duplicate side effects. The platform ensures idempotency at the application level. Each service maintains a tracking table (pre_processor_tracking, email_tracking, print_job, archival_job) that records which records have been processed. When a service receives an event, it first checks the tracking table to see if the record has already been processed. If so, the service treats the redelivery as a no-op and acknowledges the event without producing side effects.

This idempotency check is what makes the at-least-once delivery semantics of Kafka safe to use with retries. Even if Kafka delivers an event multiple times (which it will, by design, in the case of consumer failure), the service ensures that the side effects happen only once. The cost of this safety is a database lookup per event, but the lookup is on a primary key and is fast.

## When Retries Are Exhausted

When an event has been retried the maximum number of times and is still failing, the service routes it to the DLT. The DLT event includes the full original payload, the error message from the final failure, the retry count, and a timestamp. The service also writes a row to its tracking table (or a dedicated DLT tracking table) to record the final outcome. The ProcessLog entry for the DLT routing has status DLT and includes the final error message.

DLT events are not lost — they are persisted in the dlt_events table and are also visible in Splunk. The operations team reviews the DLT backlog daily and takes action on each event: either fix the underlying data and reprocess, or document the event as a known-bad record that should be discarded. The DLT is the platform's safety net: it ensures that no event is silently dropped, and it gives operations a clear, actionable list of records that need attention.

## Operational Visibility

The platform's retry behavior is fully visible in Splunk and through the MCP server. The get_retry_events tool returns all events that have been retried, with the retry count and the eventual outcome (success or DLT). A high volume of retry events indicates stress in the system: either a particular service is having trouble, or a downstream dependency is having trouble. The get_dlt_events tool shows the events that have exhausted retries and require manual intervention.

A healthy platform has a low retry rate (well under 1% of events) and a near-zero DLT rate. A spike in either metric is an early warning sign of a systemic issue. The operations team should be alerted to retry rate spikes and DLT rate spikes through the platform's monitoring system, and should investigate the root cause promptly to prevent the issue from becoming customer-visible.
