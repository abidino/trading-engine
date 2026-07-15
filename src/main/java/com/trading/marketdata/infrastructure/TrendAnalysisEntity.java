package com.trading.marketdata.infrastructure;

import com.trading.marketdata.domain.model.TrendDirection;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "technical_trend_analyses",
        uniqueConstraints = @UniqueConstraint(columnNames = {"ticker", "analysis_date"})
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrendAnalysisEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 10)
    private String ticker;

    @Column(name = "analysis_date", nullable = false)
    private LocalDate analysisDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TrendDirection trend;

    @Column(nullable = false)
    private double confidence;

    @Column(columnDefinition = "text")
    private String reasoning;

    // --- flattened indicator snapshot ---
    @Column(precision = 19, scale = 4)
    private BigDecimal closePrice;
    private Double ema9;
    private Double ema20;
    private Double ema50;
    private Double ema100;
    private Double ema200;
    private Double rsi14;
    private Double macd;
    private Double macdSignal;
    private Double macdHistogram;
    private int dataPoints;

    @Column(length = 100)
    private String llmModel;

    @Column(nullable = false)
    private Instant createdAt;
}
