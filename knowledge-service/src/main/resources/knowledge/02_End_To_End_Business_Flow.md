# End-to-End Business Flow

A TraceMind business job begins its lifecycle when a CSV file is uploaded to the file-upload-service. This service is the entry point of the platform and is responsible for accepting files, validating their format and content, creating the job record in the database, and publishing the initial set of events to Kafka. The file-upload-service uses the file_upload_topic to publish a RecordCreatedEvent for each member row in the CSV, with one event per record. This fan-out pattern is what enables the platform to process records in parallel rather than sequentially.

## Stage 1: File Ingestion

When a CSV file arrives at the file-upload-service, the service first validates the file structure: it checks that the file is a valid CSV, that all required columns are present, that the encoding is correct, and that the file size is within acceptable limits. If validation fails, the job is marked as INVALID and no events are published. If validation succeeds, the service creates a job record in the job table with status PROCESSING, generates a unique jobId, and begins publishing RecordCreatedEvent messages to Kafka. Each event includes the jobId, recordId, memberId, and the raw record data. The service continues publishing until all rows have been processed, then marks the job as INGESTION_COMPLETE.

## Stage 2: Pre-Processing

The pre-processor-service consumes from the file_upload_topic. For each RecordCreatedEvent, it applies the business validation rules: it checks that the memberId is non-empty, that the email address is well-formed (if present), that the mobile number matches the expected format, that the communication preference is one of the valid values, and that any required business fields are populated. Records that pass validation are published to either the email_request_topic or print_request_topic based on the member's communication preference. Records that fail validation are published to the dlt_topic with a detailed error message explaining which rule was violated.

The pre-processor-service is designed to be idempotent: if a record is redelivered (for example, after a Kafka consumer restart), the service recognizes the duplicate by its recordId and skips it. This is achieved by checking the pre_processor_tracking table before processing each record. If a row already exists for the recordId, the service treats the redelivery as a no-op and does not republish the event.

## Stage 3: Delivery

The email-service and print-service are independent consumers that process records in parallel. The email-service consumes from the email_request_topic, renders the appropriate email template with the member's data, and sends the email through the configured SMTP server. It writes a row to the email_tracking table for each attempt, with status SUCCESS or FAILED. The print-service consumes from the print_request_topic, generates a print-ready document for the member, and submits it to the third-party print vendor's API. It writes a row to the print_job table for each submission.

Both delivery services implement the retry policy: if a delivery fails due to a transient error (network timeout, temporary SMTP issue, rate limit from the print vendor), the event is retried with exponential backoff. The retry count is captured in each tracking table and in the Splunk logs. If retries are exhausted, the event is published to the dlt_topic.

## Stage 4: Archival

After successful delivery (or after a final delivery failure), the archival-service moves the record's data to long-term storage. The archival-service is intentionally placed at the end of the pipeline to ensure that archival only happens for records whose delivery has been definitively resolved. This prevents archiving records that are still in flight or that have been routed to the DLT for manual intervention.

The archival-service consumes from the archival_request_topic, copies the record's data (including any delivery metadata) to the archival store, and writes a row to the archival_job table. The original record's status is updated to COMPLETED in the record table once archival is successful.

## Observability Throughout

At every stage of this flow, the ProcessLog model captures a structured event with the jobId, recordId, service, stage, status, and timestamp. These events flow to Splunk in near real-time, where they are indexed and made queryable. The MCP server provides Claude Desktop with tools to query this data: get_job_timeline to see all events for a specific job in chronological order, get_record_timeline to trace a single record's journey, get_failed_jobs to see system-wide failures, and several other tools for aggregate analysis.

The end-to-end flow is designed so that every record has a definitive outcome. A record is either successfully delivered, successfully archived, or sitting in the DLT awaiting manual intervention. There is no in-between state, and there are no silently dropped records. This is the platform's core promise to the business: every record is accounted for, and every record's status is queryable through the standard observability tools.
