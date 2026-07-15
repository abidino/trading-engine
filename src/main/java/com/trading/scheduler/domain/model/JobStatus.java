package com.trading.scheduler.domain.model;

/** Execution status of a scheduled job. */
public enum JobStatus {
    RUNNING,
    SUCCESS,
    PARTIAL,
    FAILED
}
