package com.trading.scheduler.domain.port.out;

import com.trading.scheduler.domain.model.JobExecutionLog;

import java.util.List;

/** Outbound port: job execution log persistence. */
public interface JobExecutionLogRepository {
    JobExecutionLog save(JobExecutionLog log);
    List<JobExecutionLog> findAll();
    List<JobExecutionLog> findByJobName(String jobName);
}
