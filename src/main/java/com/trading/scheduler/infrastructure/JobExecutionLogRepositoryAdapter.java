package com.trading.scheduler.infrastructure;

import com.trading.scheduler.domain.model.JobExecutionLog;
import com.trading.scheduler.domain.port.out.JobExecutionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JobExecutionLogRepositoryAdapter implements JobExecutionLogRepository {

    private final JpaJobExecutionLogRepository jpa;

    @Override
    public JobExecutionLog save(JobExecutionLog log) {
        return toDomain(jpa.save(toEntity(log)));
    }

    @Override
    public List<JobExecutionLog> findAll() {
        return jpa.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<JobExecutionLog> findByJobName(String jobName) {
        return jpa.findByJobName(jobName).stream().map(this::toDomain).toList();
    }

    private JobExecutionLog toDomain(JobExecutionLogEntity e) {
        JobExecutionLog log = new JobExecutionLog(e.getId(), e.getJobName(), e.getTriggeredAt());
        if (e.getCompletedAt() != null) {
            if ("FAILED".equals(e.getStatus())) log.fail(e.getErrorDetails());
            else log.complete(e.getTickersProcessed(), e.getDecisionsProduced());
        }
        return log;
    }

    private JobExecutionLogEntity toEntity(JobExecutionLog l) {
        return JobExecutionLogEntity.builder()
                .id(l.getId()).jobName(l.getJobName()).triggeredAt(l.getTriggeredAt())
                .completedAt(l.getCompletedAt()).status(l.getStatus().name())
                .tickersProcessed(l.getTickersProcessed()).decisionsProduced(l.getDecisionsProduced())
                .errorDetails(l.getErrorDetails())
                .build();
    }
}
