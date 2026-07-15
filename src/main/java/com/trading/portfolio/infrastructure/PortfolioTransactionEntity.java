package com.trading.portfolio.infrastructure;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "portfolio_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PortfolioTransactionEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 10)
    private String ticker;

    @Column(nullable = false, length = 10)
    private String transactionType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal commission;

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant executedAt;
}
