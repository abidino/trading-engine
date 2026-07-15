package com.trading.discovery.infrastructure;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "discovery_dismissals",
        uniqueConstraints = @UniqueConstraint(columnNames = "ticker")
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DismissedTickerEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 10)
    private String ticker;

    @Column(columnDefinition = "text")
    private String reason;

    /** Why the ticker is suppressed: "DISMISSED" (user) or "AUTO" (not an up-trend). */
    @Column(length = 20)
    private String suppressionType;

    /** Suppression deadline; the ticker is re-screenable again after this instant. Null = permanent. */
    private Instant suppressedUntil;

    @Column(nullable = false)
    private Instant dismissedAt;
}
