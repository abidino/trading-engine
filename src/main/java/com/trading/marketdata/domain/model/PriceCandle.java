package com.trading.marketdata.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Value Object: a single OHLCV price candle.
 */
public record PriceCandle(
        UUID id,
        String ticker,
        LocalDate candleDate,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        long volume
) {
    public static PriceCandle create(String ticker, LocalDate date,
                                     BigDecimal open, BigDecimal high,
                                     BigDecimal low, BigDecimal close, long volume) {
        return new PriceCandle(UUID.randomUUID(), ticker, date, open, high, low, close, volume);
    }
}
