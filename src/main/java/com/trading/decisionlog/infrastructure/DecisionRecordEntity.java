package com.trading.decisionlog.infrastructure;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "decision_records")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DecisionRecordEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 10)
    private String ticker;

    @Column(nullable = false, length = 30)
    private String decisionType;

    @Column(nullable = false, length = 30)
    private String action;

    @Column(nullable = false)
    private double confidence;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reasoning;

    @Column(columnDefinition = "TEXT")
    private String technicalSummary;

    @Column(columnDefinition = "TEXT")
    private String fundamentalSummary;

    @Column(columnDefinition = "TEXT")
    private String newsSummary;

    @Column(columnDefinition = "TEXT")
    private String socialSummary;

    @Column(columnDefinition = "TEXT")
    private String counterThesis;

    @Column(columnDefinition = "TEXT")
    private String keyRisks;

    private Double entryLow;
    private Double entryHigh;
    private Double aggressiveEntry;
    private Double idealEntry;
    private Double safeEntry;
    private Double stopLoss;
    private Double takeProfit;
    private Double nearestSupport;
    private Double nearestResistance;

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant decidedAt;

    @Column(nullable = false, length = 20)
    private String outcome;

    @Column(columnDefinition = "TIMESTAMPTZ")
    private Instant evaluatedAt;
}
