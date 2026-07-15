package com.trading.marketdata.infrastructure;

import com.trading.marketdata.domain.model.MarketSession;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "intraday_quotes", indexes = {
        @Index(name = "idx_intraday_ticker_captured", columnList = "ticker, capturedAt")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IntradayQuoteEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 10)
    private String ticker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MarketSession session;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal price;

    @Column(precision = 18, scale = 4)
    private BigDecimal previousClose;

    @Column(precision = 18, scale = 4)
    private BigDecimal change;

    @Column
    private Double changePercent;

    @Column(nullable = false)
    private long volume;

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant quoteTime;

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant capturedAt;
}
