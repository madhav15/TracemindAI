package com.tracemind.mcp.query;

public interface SplunkQueryBuilder {

    String buildJobTimelineQuery(String jobId);

    String buildRecordTimelineQuery(String recordId);

    String buildFailedJobsQuery();

    String buildFailedEmailsQuery();

    String buildRetryEventsQuery();

    String buildDltEventsQuery();

    String buildEventsByServiceQuery();

    String buildEventsByStageQuery();

    String buildProcessingDurationQuery();

    String buildJobSummaryQuery(String jobId);
}
