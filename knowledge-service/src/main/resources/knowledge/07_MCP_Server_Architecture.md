# MCP Server Architecture

The MCP (Model Context Protocol) server is the integration layer between the TraceMind platform's operational data and Claude Desktop. It exposes a set of focused query tools that Claude can invoke during conversation, allowing operators to ask natural-language questions about their batch processing jobs and receive answers synthesized from live Splunk data. The server is built on Spring Boot with the Spring AI MCP server starter, and it runs in stdio mode for direct integration with Claude Desktop.

## Protocol Implementation

The MCP server implements the Model Context Protocol, which is Anthropic's standard for connecting AI assistants to external tools and data sources. The protocol defines a JSON-RPC-based message format for tool registration, tool invocation, and result return. When Claude Desktop starts, it launches the MCP server as a subprocess and exchanges a series of initialization messages. The MCP server responds with a list of available tools, their descriptions, and their input schemas. Claude uses this information to decide which tool to invoke when the user asks a question that requires external data.

The Spring AI MCP server starter handles the protocol implementation details, including message framing, request routing, and error responses. Developers only need to define the tool methods using the `@Tool` annotation, and the starter takes care of the rest. This makes adding a new tool a matter of writing a single Java method — no protocol code, no JSON serialization logic, no error handling boilerplate.

## Tool Definition

Each MCP tool is a Java method on a Spring component, annotated with `@Tool` and a clear description. The description is the most important part of the tool definition: it tells Claude when to use the tool, what it returns, and when not to use it. A well-written tool description dramatically improves Claude's ability to select the right tool for a given question.

The current MCP server exposes ten tools, organized around the platform's primary use cases. The job investigation tools (get_job_timeline, get_record_timeline, get_job_summary) answer questions about specific jobs and records. The failure analysis tools (get_failed_jobs, get_failed_emails, get_retry_events, get_dlt_events) answer questions about system-wide failures. The aggregate analysis tools (get_events_by_service, get_events_by_stage, get_processing_duration) answer questions about system-wide activity patterns.

## Query Execution

When Claude invokes a tool, the MCP server builds the corresponding Splunk query, executes it against the configured Splunk instance, and returns the results. The query construction is handled by the SplunkQueryBuilder interface, which has a concrete implementation (DefaultSplunkQueryBuilder) that produces well-formed SPL queries. The query execution is handled by the SplunkClient, which is an HTTP client wrapper around Splunk's REST API.

The query parameters are validated before the query is built. For tools that take a jobId or recordId, the parameter is checked for non-blankness. For tools that take no parameters, no validation is needed. Validation failures result in a clear error message returned to Claude, which it can use to ask the user for clarification or to correct the call.

## Connection Management

The MCP server establishes its Splunk connection on startup. The connection is configured through environment variables: SPLUNK_BASE_URL (the URL of the Splunk instance), SPLUNK_USERNAME (the service account username), SPLUNK_PASSWORD (the service account password), SPLUNK_INDEX (the index to query, defaulting to "tracemind"), and SPLUNK_VERIFY_SSL (whether to verify the Splunk server's SSL certificate, defaulting to false for self-signed certificates in development).

The connection is held for the lifetime of the MCP server process. The server is designed to be long-running: Claude Desktop launches it once and keeps it running for the duration of the conversation session. If the connection to Splunk is lost, the server attempts to reconnect transparently. If the reconnection fails, the affected tool calls return an error to Claude, which can then inform the user.

## Why Standalone

The MCP server is intentionally a standalone service, separate from the other TraceMind services. This separation has several benefits. First, the MCP server has no Kafka dependencies — it only needs Splunk access, which simplifies its deployment and reduces its operational footprint. Second, the MCP server can be updated and restarted independently of the data processing services, allowing quick iteration on tool definitions without disrupting the platform's processing pipeline. Third, the MCP server is read-only: it never writes to Splunk, never publishes to Kafka, and never modifies the platform's state. This makes it safe to grant broad access to the MCP server's tools without worrying about unintended side effects.

The standalone design also makes the MCP server easy to extend. Adding a new tool is a matter of adding a new Java method, building the JAR, and restarting the server. No database migrations, no Kafka topic additions, no changes to other services. This is what enables the platform's conversational operations to evolve quickly as new operational questions arise.

## Future Direction

The next phase of the MCP server's evolution is the integration with the knowledge-service. Once the knowledge-service has indexed the platform's documentation, the MCP server can expose a tool that performs semantic search over the documentation. This will allow Claude to answer questions about how the platform works ("how does the retry policy work?", "what is the DLT policy?") in addition to questions about the platform's current state ("show me the failures from yesterday"). The combination of operational data and documentation search will make Claude a comprehensive support assistant for the platform.
