package com.trading.marketdata.infrastructure;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "technical_signals")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TechnicalSignalEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 10)
    private String ticker;

    @Column(nullable = false, length = 50)
    private String indicatorName;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal value;

    @Column(nullable = false)
    private LocalDate signalDate;
}
