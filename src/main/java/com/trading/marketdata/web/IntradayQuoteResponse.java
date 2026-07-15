package com.trading.marketdata.web;

import com.trading.marketdata.domain.model.IntradayQuote;

import java.math.BigDecimal;
import java.time.Instant;

public record IntradayQuoteResponse(
        String ticker,
        String session,
        BigDecimal price,
        BigDecimal previousClose,
        BigDecimal change,
        Double changePercent,
        long volume,
        Instant quoteTime,
        Instant capturedAt
) {
    public static IntradayQuoteResponse from(IntradayQuote q) {
        return new IntradayQuoteResponse(
                q.ticker(),
                q.session().name(),
                q.price(),
                q.previousClose(),
                q.change(),
                q.changePercent(),
                q.volume(),
                q.quoteTime(),
                q.capturedAt());
    }
}
