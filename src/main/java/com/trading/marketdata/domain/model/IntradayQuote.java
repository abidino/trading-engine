package com.trading.marketdata.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

/**
 * Value Object: a point-in-time intraday price snapshot for a ticker, tagged with
 * the trading session it was captured in (pre-market / regular / post / closed).
 *
 * These are stored intraday (refreshed every ~10 min) and kept separately from
 * the daily OHLC candles used for technical analysis.
 */
public record IntradayQuote(
        UUID id,
        String ticker,
        MarketSession session,
        BigDecimal price,
        BigDecimal previousClose,
        BigDecimal change,
        Double changePercent,
        long volume,
        Instant quoteTime,
        Instant capturedAt
) {
    public static IntradayQuote create(String ticker, MarketSession session,
                                       BigDecimal price, BigDecimal previousClose,
                                       long volume, Instant quoteTime) {
        BigDecimal change = BigDecimal.ZERO;
        Double changePercent = 0.0;
        if (price != null && previousClose != null && previousClose.signum() != 0) {
            change = price.subtract(previousClose);
            changePercent = change.divide(previousClose, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
        }
        return new IntradayQuote(UUID.randomUUID(), ticker, session, price, previousClose,
                change, changePercent, volume,
                quoteTime != null ? quoteTime : Instant.now(), Instant.now());
    }
}
