package com.tracemind.mcp.query;

import com.tracemind.mcp.config.SplunkProperties;
import org.springframework.stereotype.Component;

@Component
public class DefaultSplunkQueryBuilder implements SplunkQueryBuilder {

    private final SplunkProperties splunkProperties;

    public DefaultSplunkQueryBuilder(SplunkProperties splunkProperties) {
        this.splunkProperties = splunkProperties;
    }

    @Override
    public String buildJobTimelineQuery(String jobId) {
        validateNotBlank(jobId, "jobId");
        return String.format("search index=%s jobId=\"%s\"\n| sort 0 _time", splunkProperties.getIndex(), jobId);
    }

    @Override
    public String buildRecordTimelineQuery(String recordId) {
        validateNotBlank(recordId, "recordId");
        return String.format("search index=%s recordId=\"%s\"\n| sort 0 _time", splunkProperties.getIndex(), recordId);
    }

    @Override
    public String buildFailedJobsQuery() {
        return String.format("search index=%s status=\"FAILED\"", splunkProperties.getIndex());
    }

    @Override
    public String buildFailedEmailsQuery() {
        return String.format("search index=%s service=\"email-service\" status=\"FAILED\"", splunkProperties.getIndex());
    }

    @Override
    public String buildRetryEventsQuery() {
        return String.format("search index=%s retryCount>0", splunkProperties.getIndex());
    }

    @Override
    public String buildDltEventsQuery() {
        return String.format("search index=%s action=\"DLT\"", splunkProperties.getIndex());
    }

    @Override
    public String buildEventsByServiceQuery() {
        return String.format("search index=%s\n| stats count by service", splunkProperties.getIndex());
    }

    @Override
    public String buildEventsByStageQuery() {
        return String.format("search index=%s\n| stats count by stage", splunkProperties.getIndex());
    }

    @Override
    public String buildProcessingDurationQuery() {
        return String.format("search index=%s", splunkProperties.getIndex());
    }

    @Override
    public String buildJobSummaryQuery(String jobId) {
        validateNotBlank(jobId, "jobId");
        return String.format("search index=%s jobId=\"%s\"\n| sort 0 _time", splunkProperties.getIndex(), jobId);
    }

    private void validateNotBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be null or blank");
        }
    }
}
