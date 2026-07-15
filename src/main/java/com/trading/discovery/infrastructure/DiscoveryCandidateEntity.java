package com.trading.discovery.infrastructure;

import com.trading.discovery.domain.model.DiscoveryStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "discovery_candidates",
        uniqueConstraints = @UniqueConstraint(columnNames = "ticker")
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DiscoveryCandidateEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 10)
    private String ticker;

    @Column(length = 200)
    private String companyName;

    @Column(length = 100)
    private String sector;

    @Column(length = 50)
    private String screenerSource;

    /** Raw screener columns serialized as JSON. */
    @Column(columnDefinition = "text")
    private String criteriaJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscoveryStatus status;

    private boolean recommended;

    private Double confidence;

    @Column(columnDefinition = "text")
    private String reasoning;

    @Column(length = 30)
    private String trendDirection;

    @Column(nullable = false)
    private Instant discoveredAt;

    private Instant evaluatedAt;
}
