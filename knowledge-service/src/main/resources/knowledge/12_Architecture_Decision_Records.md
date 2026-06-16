# Architecture Decision Records

This document collects the significant architecture decisions made during the design and evolution of the TraceMind platform. Each decision record follows a standard format: the context that led to the decision, the decision itself, the consequences (both positive and negative), and the alternatives that were considered. The records are numbered chronologically, with newer records at the end. This is the platform's institutional memory for "why we did it this way" and is the first place to look when considering a change to the existing architecture.

## ADR-001: Use Kafka for Asynchronous Service-to-Service Communication

Context: The platform needs to process large volumes of member records with high throughput and fault tolerance. Synchronous HTTP calls between services would create tight coupling and make the system fragile to individual service failures.

Decision: All service-to-service communication happens through Kafka topics. Services publish events to topics and consume from topics. There are no synchronous inter-service calls within TraceMind.

Consequences: Positive: services are decoupled, can be scaled independently, and are resilient to individual service failures. Negative: the platform has a dependency on Kafka infrastructure, which adds operational complexity. The asynchronous nature also makes debugging harder in some cases (no synchronous call stack to trace).

Alternatives considered: Synchronous REST calls (rejected: too fragile), gRPC streaming (rejected: still synchronous, more complex), database-as-queue (rejected: anti-pattern, no backpressure).

## ADR-002: Use Splunk as the Central Observability Store

Context: The platform needs unified observability across all services. Each service produces log events, and operators need to be able to search and correlate events across services to debug issues.

Decision: All services emit structured ProcessLog entries to stdout, and a centralized log shipper forwards these entries to a Splunk index. Splunk is the single source of truth for observability data.

Consequences: Positive: unified query interface, powerful search language, good visualization tools. Negative: Splunk licensing cost, operational dependency on Splunk availability, performance overhead of log shipping.

Alternatives considered: ELK stack (rejected: more operational overhead), CloudWatch (rejected: vendor lock-in), custom logging service (rejected: would need to build search and visualization).

## ADR-003: Standardize on the ProcessLog Model for All Event Logging

Context: Without a standardized log format, each service would emit logs in its own format, making cross-service queries difficult and requiring per-service query patterns.

Decision: Every service emits logs in the ProcessLog format with the six core fields (jobId, recordId, service, stage, status, timestamp). The format is enforced through a shared logging library in the common-lib module.

Consequences: Positive: uniform query interface, predictable log structure, easy cross-service correlation. Negative: slight coupling to the shared log schema (any change to the core fields requires coordination across all services).

Alternatives considered: Free-form logging (rejected: makes queries brittle), OpenTelemetry (considered: would be more standards-compliant but adds complexity for the current use case).

## ADR-004: Use Spring Boot 3.3 with Java 21

Context: The platform needs a modern application framework with good support for reactive and concurrent programming, and good integration with the JVM ecosystem.

Decision: All services are built on Spring Boot 3.3 with Java 21. The platform leverages Java 21 features like virtual threads, pattern matching, and records where they provide clear benefits.

Consequences: Positive: modern language features, good performance, large ecosystem. Negative: requires team familiarity with newer Java idioms, some libraries may not be fully compatible.

Alternatives considered: Spring Boot 2.x with Java 11 (rejected: missing features), Quarkus (rejected: smaller ecosystem), Micronaut (rejected: less mature for our use case).

## ADR-005: Build MCP Server as a Standalone Module That Queries Splunk

Context: The platform needs an AI integration layer that allows Claude Desktop to access operational data. The integration should not be tightly coupled to the data processing services.

Decision: The mcp-server is a standalone module that queries Splunk. It has no dependencies on the other TraceMind services and no Kafka or database dependencies. It runs in stdio mode for direct integration with Claude Desktop.

Consequences: Positive: decoupled from the data pipeline, easy to update and restart independently, no impact on the processing services. Negative: extra hop for data retrieval (Claude → MCP → Splunk), potential latency for complex queries.

Alternatives considered: Direct database access from Claude (rejected: would require exposing the database to the AI), embedding AI capabilities in each service (rejected: would require duplicating logic across services).

## ADR-006: Use pgvector for Semantic Search Over Knowledge Documents

Context: The platform needs to provide semantic search over its own documentation to enable AI-assisted support. The search should return relevant documents based on natural-language queries.

Decision: Use pgvector, the PostgreSQL extension for vector similarity search. Store document chunks and their embeddings in a dedicated table (knowledge_document), and query using cosine similarity. The knowledge-service module handles ingestion and search.

Consequences: Positive: leverages existing PostgreSQL infrastructure, no need for a separate vector database, consistent with the platform's data ownership model. Negative: pgvector has performance limitations for very large document collections (millions of chunks), requires the pgvector extension to be installed.

Alternatives considered: Pinecone (rejected: vendor lock-in, additional cost), Weaviate (rejected: additional infrastructure), in-memory vector search (rejected: not durable, doesn't survive restarts).

## ADR-007: Use OpenAI text-embedding-3-small for Embeddings

Context: The knowledge-service needs an embedding model to convert document chunks into vectors. The model should balance cost, speed, and quality.

Decision: Use OpenAI's text-embedding-3-small model with 1536 dimensions. This model provides good retrieval quality for documentation use cases at a low cost per embedding.

Consequences: Positive: high-quality embeddings, low cost, fast API response. Negative: dependency on OpenAI's API availability, ongoing API cost, embeddings are not portable to other models without re-embedding.

Alternatives considered: OpenAI text-embedding-3-large (rejected: higher cost without clear quality benefit for our use case), open-source models like sentence-transformers (rejected: would require hosting infrastructure), Cohere embeddings (rejected: would add another vendor dependency).

## ADR-008: Use Heading-Based Chunking for Documents

Context: The knowledge-service needs to split documents into chunks for embedding. The chunking strategy should preserve semantic meaning and produce chunks of consistent size.

Decision: Split documents by markdown headings (lines starting with #), with each heading starting a new chunk. Accumulate sections until the chunk reaches the target word count (500-1200 words), then start a new chunk. This preserves semantic boundaries while keeping chunk size manageable.

Consequences: Positive: chunks respect document structure, semantic meaning is preserved, chunk size is bounded. Negative: documents without headings produce a single large chunk, very long sections may exceed the maximum word count.

Alternatives considered: Fixed-size chunking (rejected: breaks semantic boundaries), sentence-based chunking (rejected: too granular, increases embedding cost), LLM-based chunking (rejected: too expensive and slow for the use case).

## ADR-009: Wire Knowledge Ingestion to MCP Server Startup

Context: The knowledge-service needs a guaranteed execution path for initial ingestion. A standalone runner in the knowledge-service would not be invoked, since the knowledge-service is a library module, not a runnable service.

Decision: Add a startup hook to the mcp-server that invokes the knowledge-service's ingestion method on application startup. The hook is gated by a configuration flag (knowledge.ingest-on-startup) so it can be disabled in production where ingestion has already been done.

Consequences: Positive: guaranteed ingestion path, no need to create a separate runnable service, simple to operate. Negative: ingestion happens on every mcp-server restart (gated by the configuration flag), couples the two modules' deployment lifecycle.

Alternatives considered: Separate runnable service (rejected: adds deployment complexity), scheduled task (rejected: adds infrastructure), manual command-line invocation (rejected: easy to forget).
