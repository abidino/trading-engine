package com.trading.discovery.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Aggregate: a persisted stock candidate surfaced by the screener and evaluated
 * by the LLM purely on technical posture + screener fundamentals (no news/social).
 */
public record DiscoveryCandidate(
        UUID id,
        String ticker,
        String companyName,
        String sector,
        String screenerSource,
        Map<String, String> matchedCriteria,
        DiscoveryStatus status,
        boolean recommended,
        Double confidence,
        String reasoning,
        String trendDirection,
        Instant discoveredAt,
        Instant evaluatedAt
) {
    public DiscoveryCandidate {
        matchedCriteria = matchedCriteria != null ? Map.copyOf(matchedCriteria) : Map.of();
    }

    /** Freshly screened, not yet evaluated. */
    public static DiscoveryCandidate screened(String ticker, String companyName, String sector,
                                              String source, Map<String, String> criteria) {
        return new DiscoveryCandidate(
                UUID.randomUUID(), ticker, companyName, sector, source, criteria,
                DiscoveryStatus.SCREENED, false, null, null, null,
                Instant.now(), null);
    }

    /** Returns a copy carrying the LLM evaluation result. */
    public DiscoveryCandidate withEvaluation(boolean recommended, double confidence,
                                             String reasoning, String trendDirection) {
        return new DiscoveryCandidate(
                id, ticker, companyName, sector, screenerSource, matchedCriteria,
                recommended ? DiscoveryStatus.RECOMMENDED : DiscoveryStatus.NOT_RECOMMENDED,
                recommended, confidence, reasoning, trendDirection,
                discoveredAt, Instant.now());
    }

    /** Returns a copy with a new status (e.g. PROMOTED / DISMISSED). */
    public DiscoveryCandidate withStatus(DiscoveryStatus newStatus) {
        return new DiscoveryCandidate(
                id, ticker, companyName, sector, screenerSource, matchedCriteria,
                newStatus, recommended, confidence, reasoning, trendDirection,
                discoveredAt, evaluatedAt);
    }
}
