# Kafka Event Flow

Kafka is the asynchronous backbone of the TraceMind platform. Every service-to-service communication happens through Kafka topics, with the producer publishing events and one or more consumers processing them. The platform uses Kafka for three reasons: it decouples services, it provides durable storage of events, and it enables backpressure through consumer lag monitoring. The platform's event flow is intentionally unidirectional — events flow forward through the pipeline, and the only way to recover from a failure is to either retry, route to the DLT, or manually re-inject the event at the appropriate stage.

## Topic Structure

The platform uses a small number of well-named topics, each representing a logical stage boundary. The file_upload_topic carries RecordCreatedEvent messages published by the file-upload-service after successful ingestion. The pre_processor_tracking_topic carries events that record the outcome of pre-processing for each record, used for observability and idempotency. The email_request_topic carries events destined for the email-service. The print_request_topic carries events destined for the print-service. The archival_request_topic carries events destined for the archival-service. The dlt_topic is the terminal destination for events that cannot be processed after all retries.

Each topic is configured with multiple partitions to enable parallel processing. The partition count is a deployment-time configuration that can be tuned based on the platform's throughput requirements. A higher partition count allows more consumers to process events in parallel, but it also increases the overhead of Kafka's coordinator. The current production configuration uses 12 partitions for the high-volume topics (file_upload_topic, email_request_topic, print_request_topic) and 6 partitions for the lower-volume topics.

## Event Schema

Every event in the platform follows a common envelope structure that includes the eventId (a UUID for deduplication), the eventType (a string identifying the event class), the timestamp (when the event was created), the jobId, the recordId, the source service, and the event-specific payload. The envelope is defined in the common-lib module as a base class that all specific event types extend. This means consumers can deserialize any event from any topic using the same base class, and then downcast to the specific event type if they need the payload.

The RecordCreatedEvent is the most common event type. It is published by the file-upload-service and consumed by the pre-processor-service. It contains the memberId, the name, the email, the mobile number, the communication preference, and any other member data from the source CSV. The pre-processor-service uses these fields to validate the record and determine the routing.

## Consumer Groups

Each service that consumes from Kafka uses a distinct consumer group, so that each event is delivered to exactly one instance of each service. For example, all instances of the email-service join the email-service-consumer-group, and Kafka ensures that each event from the email_request_topic is delivered to exactly one of those instances. This is what enables the services to scale horizontally: adding a new instance of the email-service automatically shares the load with the existing instances.

Within a consumer group, the assignment of partitions to consumers is managed by Kafka's group coordinator. When a new consumer joins the group, the coordinator rebalances the partition assignments. During a rebalance, the consumers pause processing, the partitions are reassigned, and processing resumes. The rebalance is fast (typically under a second for small consumer groups) but it does cause a brief pause in event processing. The platform's retry policy is designed to handle this: any in-flight events that were being processed when the rebalance started will be redelivered to the new partition owner.

## Retry and Backpressure

When a consumer fails to process an event due to a transient error, the event is republished to a retry topic with a delay. The retry topic uses Kafka's delayed delivery feature (or, in the absence of that feature, a scheduled task that republishes the event after the delay). The retry count is stored in the event envelope and in the consumer's tracking table. If the retry count exceeds the configured maximum, the event is published to the DLT.

The platform's backpressure mechanism is consumer lag monitoring. If a consumer is falling behind (its lag is growing), it indicates that the producer is publishing faster than the consumer can process. The platform's deployment configuration includes alerts for consumer lag exceeding a threshold, which trigger automatic scaling of the consumer service. This is what keeps the platform stable under load spikes: the consumers scale up to match the producer rate, and they scale back down when the load subsides.

## Observability of Event Flow

Every event that flows through Kafka is also logged to Splunk via the producing and consuming services. The log entries include the topic name, the partition, the offset, the eventId, and the service that produced or consumed the event. This means that the full event flow can be reconstructed from Splunk logs, even if Kafka's own logs are not available. The MCP server's get_job_timeline and get_record_timeline tools both rely on this observability to provide chronological views of events.

The platform also tracks Kafka's own metrics: throughput, latency, consumer lag, partition count, and broker health. These metrics are exposed through standard JMX and are scraped by the platform's monitoring system. Alerts are configured for critical metrics (broker unavailability, partition under-replication, consumer lag exceeding threshold) to ensure that Kafka issues are detected and resolved quickly.
