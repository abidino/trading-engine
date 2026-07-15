package com.trading.watchlist.infrastructure;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "watchlist_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WatchlistItemEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 10, unique = true)
    private String ticker;

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant addedAt;

    @Column(precision = 19, scale = 4)
    private BigDecimal targetPrice;

    @Column(precision = 19, scale = 4)
    private BigDecimal stopLoss;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private boolean approved;
}
