package com.trading.discovery.domain.model;

import com.trading.shared.kernel.Ticker;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Entity: a stock candidate surfaced by the screener during a discovery cycle.
 * Transient — not permanently stored; replaced each discovery run.
 */
public record PotentialStock(
        UUID id,
        Ticker ticker,
        String screenerSource,
        Instant discoveredAt,
        Map<String, String> matchedCriteria
) {
    public static PotentialStock create(Ticker ticker, String source, Map<String, String> criteria) {
        return new PotentialStock(UUID.randomUUID(), ticker, source, Instant.now(),
                criteria != null ? Map.copyOf(criteria) : Map.of());
    }
}
