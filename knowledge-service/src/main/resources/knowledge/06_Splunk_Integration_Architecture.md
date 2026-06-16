# Splunk Integration Architecture

The Splunk integration is the observability layer of the TraceMind platform. Every service writes structured ProcessLog entries to stdout, and a centralized log shipper forwards these entries to a Splunk index where they are available for search and aggregation. The mcp-server module then exposes this data to Claude Desktop through the Model Context Protocol, enabling conversational operations on top of the platform's event stream. This three-tier architecture (services → Splunk → MCP server → Claude) is what makes the platform's data accessible to both human operators and AI assistants.

## Log Shipping

The platform uses a sidecar log shipper (typically Fluent Bit) running alongside each service. The log shipper tails the service's stdout, parses each line as JSON, and forwards the parsed events to Splunk's HTTP Event Collector (HEC). The HEC endpoint is configured to write to the platform's shared Splunk index. This indirection — services write to stdout, log shipper forwards to Splunk — means the services do not need to know about Splunk's existence, and Splunk's configuration changes do not require service changes.

The log shipper handles transient Splunk unavailability through local buffering. If Splunk is unreachable, the log shipper queues events on local disk and retries forwarding when Splunk becomes available again. The buffer is bounded (typically 1 GB) and old events are dropped if the buffer fills up. This is acceptable for the TraceMind use case because the operational state is also recorded in the services' own databases, and Splunk is used primarily for query and visualization rather than as the system of record.

## Index Configuration

The platform uses a single Splunk index called `tracemind` for all ProcessLog entries. A single index simplifies cross-service queries: an operator can search for events from any service or any stage without needing to know which index to query. The trade-off is that the index grows quickly (the platform produces millions of events per day at peak), so the index retention policy is set to 30 days for hot storage and 90 days for warm storage. Events older than 90 days are archived to cold storage and are still queryable but with longer search times.

Within the index, the events are stored with their full JSON structure, including the ProcessLog core fields and any service-specific fields. Splunk automatically extracts the top-level fields (jobId, recordId, service, stage, status, timestamp) as indexed fields, enabling fast queries on these dimensions. The service-specific fields (like error codes, retry counts, processing durations) are also indexed but may require additional configuration for very large cardinality fields.

## MCP Server Integration

The mcp-server is a Spring Boot application that uses the Spring AI MCP server starter to expose its functionality as tools to Claude Desktop. Each tool is a Java method annotated with `@Tool` and backed by a service that builds and executes a Splunk SPL (Search Processing Language) query. The query results are returned to Claude in JSON format, which Claude can then format into a natural-language response for the user.

The MCP server runs in stdio mode, meaning it communicates with Claude Desktop over standard input/output rather than over HTTP. This is the recommended mode for local Claude Desktop integration: the user installs the MCP server as a local process, and Claude Desktop launches it on demand. The Splunk connection details are passed to the MCP server through environment variables, allowing the same binary to be used against different Splunk instances (development, staging, production).

## Tool to Query Mapping

Each MCP tool maps to a specific Splunk query. The get_job_timeline tool runs `search index=tracemind jobId="X" | sort _time` and returns all events for the specified job. The get_record_timeline tool runs the same query but filters on recordId. The get_failed_jobs tool runs `search index=tracemind status="FAILED"` to return all FAILED events. The get_failed_emails tool runs `search index=tracemind service="email-service" status="FAILED"`. The get_retry_events tool runs `search index=tracemind retryCount>0`. The get_dlt_events tool runs `search index=tracemind action="DLT"`. The get_events_by_service and get_events_by_stage tools use `stats count` aggregations.

The tools are intentionally narrow: each one answers a specific question. This is by design, because Claude is better at selecting the right tool when the tools are clearly differentiated. A broad "get_events" tool with many parameters would be harder for Claude to use correctly than a dozen specific tools, each with a clear purpose and a clear example of when to use it.

## Security and Access Control

The MCP server's connection to Splunk is authenticated using a service account that has read-only access to the tracemind index. The service account's credentials are stored as environment variables on the machine where the MCP server runs. The MCP server does not write to Splunk — it only reads — so the read-only permission is sufficient and minimizes the blast radius of any credential compromise.

The data exposed to Claude Desktop is operational data: job timelines, failure events, retry counts, and aggregate statistics. The data does not include member PII (names, emails, mobile numbers) in the fields that the MCP server returns. The member PII is captured in the underlying Splunk events but the MCP server's queries select only the operational fields, not the PII fields. This is an intentional design choice: the MCP server is for operational support, not for member data access.
