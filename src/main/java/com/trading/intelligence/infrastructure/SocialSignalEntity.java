package com.trading.intelligence.infrastructure;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "social_signals")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SocialSignalEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 10)
    private String ticker;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal engagementScore;

    @Column(precision = 10, scale = 4)
    private BigDecimal sentimentScore;

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant capturedAt;
}
