package com.trading.scheduler.infrastructure;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_execution_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JobExecutionLogEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String jobName;

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant triggeredAt;

    @Column(columnDefinition = "TIMESTAMPTZ")
    private Instant completedAt;

    @Column(nullable = false, length = 20)
    private String status;

    private int tickersProcessed;
    private int decisionsProduced;

    @Column(columnDefinition = "TEXT")
    private String errorDetails;
}
