package com.trading.portfolio.infrastructure;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "portfolio_positions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PortfolioPositionEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 10)
    private String ticker;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal averageEntryPrice;

    @Column(precision = 19, scale = 4)
    private BigDecimal stopLossLevel;

    @Column(nullable = false)
    private boolean active;
}
