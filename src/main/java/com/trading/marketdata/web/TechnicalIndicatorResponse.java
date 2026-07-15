package com.trading.marketdata.web;

import com.trading.marketdata.domain.model.TechnicalIndicatorSnapshot;

/** API view of a computed indicator snapshot (no LLM verdict). */
public record TechnicalIndicatorResponse(
        String ticker,
        String asOfDate,
        double close,
        Double ema9,
        Double ema20,
        Double ema50,
        Double ema100,
        Double ema200,
        Double rsi14,
        Double macd,
        Double macdSignal,
        Double macdHistogram,
        int dataPoints
) {
    public static TechnicalIndicatorResponse from(TechnicalIndicatorSnapshot s) {
        return new TechnicalIndicatorResponse(
                s.ticker(),
                s.asOfDate().toString(),
                s.close().doubleValue(),
                s.ema9(), s.ema20(), s.ema50(), s.ema100(), s.ema200(),
                s.rsi14(), s.macd(), s.macdSignal(), s.macdHistogram(),
                s.dataPoints());
    }
}
