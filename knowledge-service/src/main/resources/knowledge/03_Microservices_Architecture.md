# Microservices Architecture

The TraceMind platform is built as a collection of independent Spring Boot services, each owning a discrete responsibility and communicating through asynchronous Kafka topics. The services are organized around business capabilities rather than technical layers, which means each service is a self-contained unit that can be developed, deployed, scaled, and maintained independently. There is no shared database between services, no synchronous inter-service calls, and no implicit dependencies on the deployment topology.

## Service Inventory

The platform consists of seven services. The file-upload-service is the entry point, accepting CSV files, validating their structure, creating job records, and publishing record-created events. The pre-processor-service consumes those events, applies business validation rules, and routes records to the appropriate delivery service. The email-service handles electronic delivery through SMTP, the print-service dispatches physical mail through a third-party print vendor, and the archival-service moves completed records to long-term storage. The mcp-server exposes operational data from Splunk as tools to Claude Desktop, enabling conversational operations. The knowledge-service provides semantic search over the platform's own documentation.

All services share the common-lib module, which contains shared DTOs, event definitions, exception types, and utility classes. The shared library ensures that when one service publishes a RecordCreatedEvent, the consumer in another service deserializes the exact same structure. This is enforced at compile time, not at runtime, which catches schema mismatches during the build.

## Communication Patterns

The primary communication pattern is asynchronous event publishing through Kafka. A service that has completed its work publishes an event to a topic, and any number of downstream services can consume from that topic. The publisher does not know who the consumers are, and the consumers do not know who the publisher is — they only share the event schema. This decoupling is what enables the platform to scale horizontally: adding a new consumer for an existing event requires no changes to the publisher.

Within a service, the patterns are more conventional. Services use Spring Data JPA to interact with their own database tables. They use Spring Kafka to consume from and publish to Kafka topics. They use standard HTTP clients only for outbound calls to third-party services (SMTP, print vendor) — never for service-to-service communication within TraceMind.

## Data Ownership

Each service owns its data. The file-upload-service owns the job table. The pre-processor-service owns the pre_processor_tracking table. The email-service owns the email_tracking table. The print-service owns the print_job table. The archival-service owns the archival_job table. The knowledge-service owns the knowledge_document table. No service can read or write another service's tables directly — all cross-service data sharing happens through events on Kafka or through queryable artifacts like Splunk logs.

This data ownership model has a significant operational benefit: each service's data can be backed up, archived, and even deleted independently. If the email_tracking table grows too large, only the email-service is affected, and the retention policy for that table is configured in the email-service's own configuration. There is no central database that everyone depends on, and there is no shared schema that requires coordinated migrations.

## Deployment Topology

The services are designed to be deployed independently. In production, each service runs as its own JVM process (or its own Kubernetes pod) with its own configuration, its own logging, and its own scaling parameters. The services are stateless: any state they need is either in their own database or in Kafka. This means a service can be restarted, redeployed, or scaled out at any time without losing data or disrupting other services.

The exception to this pattern is the mcp-server, which is intentionally lightweight and runs in stdio mode (no web server). The mcp-server is designed to be invoked by Claude Desktop on demand, and its only state is the connection to Splunk, which it establishes on each invocation. This makes the mcp-server trivial to operate: no database, no message queue, no scheduler, no health check endpoint.

## Resilience and Recovery

Each service implements the retry policy for transient failures. If a service encounters a network timeout or temporary downstream unavailability, it increments the retry count and republishes the event to a retry topic with a delay. The platform uses a dead-letter topic for events that cannot be processed after all retries have been exhausted. The DLT preserves the full original payload, the error context, and the retry count, making manual recovery straightforward.

The services also implement idempotency at the application level. If a record is redelivered (for example, after a consumer restart), the service recognizes the duplicate by its primary key and treats the redelivery as a no-op. This is what makes the at-least-once delivery semantics of Kafka safe to use: even if a record is processed multiple times, the side effects happen only once.

## Observability

Every service emits structured logs that conform to the ProcessLog model. The logs are written to stdout in JSON format and are collected by the platform's log shipper, which forwards them to Splunk. The standardized log format means that operations teams can write Splunk queries that span all services — for example, "show me every event related to jobId X" or "count failures by service in the last 24 hours" — and get consistent results regardless of which service produced the event.

The mcp-server exposes this observability to Claude Desktop. With the MCP server running and connected to Claude Desktop, an operations engineer can ask questions in natural language like "show me the timeline for job JOB-123" or "how many email failures occurred yesterday" and receive answers synthesized from live Splunk data. This is the platform's bridge between human operators and the underlying event stream.
