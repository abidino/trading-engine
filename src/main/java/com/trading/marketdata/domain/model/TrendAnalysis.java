package com.trading.marketdata.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Aggregate: a persisted daily technical-trend verdict for a ticker.
 *
 * Combines the objectively computed {@link TechnicalIndicatorSnapshot} with the
 * LLM's qualitative interpretation (direction, confidence, reasoning). These rows
 * accumulate over time so the orchestration module can reason about trend history
 * (e.g. "50 günün 40'ında UPTREND").
 */
public record TrendAnalysis(
        UUID id,
        String ticker,
        LocalDate analysisDate,
        TrendDirection trend,
        double confidence,
        String reasoning,
        TechnicalIndicatorSnapshot snapshot,
        String llmModel,
        Instant createdAt
) {
    public static TrendAnalysis create(
            String ticker,
            LocalDate analysisDate,
            TrendDirection trend,
            double confidence,
            String reasoning,
            TechnicalIndicatorSnapshot snapshot,
            String llmModel) {
        return new TrendAnalysis(
                UUID.randomUUID(), ticker, analysisDate, trend, confidence,
                reasoning, snapshot, llmModel, Instant.now());
    }
}
