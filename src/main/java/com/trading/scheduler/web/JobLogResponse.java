package com.trading.scheduler.web;

import com.trading.scheduler.domain.model.JobExecutionLog;

public record JobLogResponse(
        String id, String jobName, String status,
        String startedAt, String finishedAt,
        Long durationMs, int itemsProcessed, String errorMessage
) {
    public static JobLogResponse from(JobExecutionLog l) {
        Long duration = null;
        if (l.getTriggeredAt() != null && l.getCompletedAt() != null) {
            duration = l.getCompletedAt().toEpochMilli() - l.getTriggeredAt().toEpochMilli();
        }
        return new JobLogResponse(
                l.getId().toString(), l.getJobName(), l.getStatus().name(),
                l.getTriggeredAt().toString(),
                l.getCompletedAt() != null ? l.getCompletedAt().toString() : null,
                duration,
                l.getTickersProcessed(),
                l.getErrorDetails());
    }
}
