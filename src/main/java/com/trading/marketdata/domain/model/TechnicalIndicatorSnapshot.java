package com.trading.marketdata.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Value Object: an immutable snapshot of all technical indicators computed for a
 * ticker on a single trading day.
 *
 * Indicator fields are nullable {@link Double} — when the available price history
 * is shorter than the indicator period (e.g. fewer than 200 candles for EMA-200),
 * that field is {@code null} rather than a misleading partial value.
 */
public record TechnicalIndicatorSnapshot(
        String ticker,
        LocalDate asOfDate,
        BigDecimal close,
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
) {}
