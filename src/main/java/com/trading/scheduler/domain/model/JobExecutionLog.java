package com.trading.scheduler.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity: execution record for a scheduled background job.
 */
public class JobExecutionLog {

    private final UUID id;
    private final String jobName;
    private final Instant triggeredAt;
    private Instant completedAt;
    private JobStatus status;
    private int tickersProcessed;
    private int decisionsProduced;
    private String errorDetails;

    public JobExecutionLog(UUID id, String jobName, Instant triggeredAt) {
        this.id = id;
        this.jobName = jobName;
        this.triggeredAt = triggeredAt;
        this.status = JobStatus.RUNNING;
    }

    public static JobExecutionLog start(String jobName) {
        return new JobExecutionLog(UUID.randomUUID(), jobName, Instant.now());
    }

    public void complete(int tickersProcessed, int decisionsProduced) {
        this.completedAt = Instant.now();
        this.status = JobStatus.SUCCESS;
        this.tickersProcessed = tickersProcessed;
        this.decisionsProduced = decisionsProduced;
    }

    public void partialFailure(int tickersProcessed, int decisionsProduced, String errorDetails) {
        this.completedAt = Instant.now();
        this.status = JobStatus.PARTIAL;
        this.tickersProcessed = tickersProcessed;
        this.decisionsProduced = decisionsProduced;
        this.errorDetails = errorDetails;
    }

    public void fail(String errorDetails) {
        this.completedAt = Instant.now();
        this.status = JobStatus.FAILED;
        this.errorDetails = errorDetails;
    }

    public UUID getId() { return id; }
    public String getJobName() { return jobName; }
    public Instant getTriggeredAt() { return triggeredAt; }
    public Instant getCompletedAt() { return completedAt; }
    public JobStatus getStatus() { return status; }
    public int getTickersProcessed() { return tickersProcessed; }
    public int getDecisionsProduced() { return decisionsProduced; }
    public String getErrorDetails() { return errorDetails; }
}
