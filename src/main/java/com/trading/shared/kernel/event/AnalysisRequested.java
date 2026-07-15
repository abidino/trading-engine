package com.trading.shared.kernel.event;

import com.trading.shared.kernel.AnalysisRequestType;
import com.trading.shared.kernel.DomainEvent;
import com.trading.shared.kernel.Ticker;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Cross-domain event: a domain requests that AI analysis be performed on a ticker.
 *
 * Published by: portfolio, watchlist, discovery (via scheduler jobs or direct user action).
 * Consumed by: ai-orchestration.
 *
 * Defined in shared-kernel so no domain module needs to depend on another.
 */
public record AnalysisRequested(
        Ticker ticker,
        AnalysisRequestType requestType,
        Map<String, String> contextMetadata,
        Instant occurredAt
) implements DomainEvent {

    public AnalysisRequested {
        Objects.requireNonNull(ticker, "Ticker must not be null");
        Objects.requireNonNull(requestType, "RequestType must not be null");
        contextMetadata = contextMetadata != null ? Map.copyOf(contextMetadata) : Map.of();
        occurredAt = occurredAt != null ? occurredAt : Instant.now();
    }

    public static AnalysisRequested of(Ticker ticker, AnalysisRequestType type) {
        return new AnalysisRequested(ticker, type, Map.of(), Instant.now());
    }

    public static AnalysisRequested of(Ticker ticker, AnalysisRequestType type, Map<String, String> meta) {
        return new AnalysisRequested(ticker, type, meta, Instant.now());
    }
}
