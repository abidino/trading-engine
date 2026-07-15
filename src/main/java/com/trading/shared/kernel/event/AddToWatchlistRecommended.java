package com.trading.shared.kernel.event;

import com.trading.shared.kernel.DomainEvent;
import com.trading.shared.kernel.Ticker;

import java.time.Instant;
import java.util.Objects;

/**
 * Cross-domain event: discovery analysis resulted in an ADD_TO_WATCHLIST recommendation.
 *
 * Published by: ai-orchestration (after evaluating a discovery candidate).
 * Consumed by: watchlist (creates a draft WatchlistItem pending user approval).
 */
public record AddToWatchlistRecommended(
        Ticker ticker,
        String reasoning,
        double confidence,
        Instant occurredAt
) implements DomainEvent {

    public AddToWatchlistRecommended {
        Objects.requireNonNull(ticker, "Ticker must not be null");
        Objects.requireNonNull(reasoning, "Reasoning must not be null");
        occurredAt = occurredAt != null ? occurredAt : Instant.now();
    }

    public static AddToWatchlistRecommended of(Ticker ticker, String reasoning, double confidence) {
        return new AddToWatchlistRecommended(ticker, reasoning, confidence, Instant.now());
    }
}
