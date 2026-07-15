package com.trading.shared.kernel.event;

import com.trading.shared.kernel.DomainEvent;
import com.trading.shared.kernel.Ticker;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Cross-domain event: a daily technical-trend verdict was computed and persisted.
 *
 * Published by: marketdata (after analyzing a ticker's indicators with the LLM).
 * Consumed by: notification / decision-log (and available for future reactive flows).
 */
public record TechnicalTrendComputed(
        Ticker ticker,
        LocalDate analysisDate,
        String trend,
        double confidence,
        String reasoning,
        Instant occurredAt
) implements DomainEvent {

    public TechnicalTrendComputed {
        Objects.requireNonNull(ticker, "Ticker must not be null");
        Objects.requireNonNull(trend, "Trend must not be null");
        occurredAt = occurredAt != null ? occurredAt : Instant.now();
    }

    public static TechnicalTrendComputed of(
            Ticker ticker, LocalDate analysisDate, String trend, double confidence, String reasoning) {
        return new TechnicalTrendComputed(ticker, analysisDate, trend, confidence, reasoning, Instant.now());
    }
}
