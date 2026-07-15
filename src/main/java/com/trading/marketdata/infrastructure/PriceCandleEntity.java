package com.trading.marketdata.infrastructure;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "price_candles", uniqueConstraints = @UniqueConstraint(columnNames = {"ticker","candle_date"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PriceCandleEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 10)
    private String ticker;

    @Column(nullable = false)
    private LocalDate candleDate;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal open;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal high;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal low;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal close;

    @Column(nullable = false)
    private long volume;
}
