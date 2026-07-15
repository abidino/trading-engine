package com.trading.discovery.domain.model;

/**
 * Lifecycle status of a discovery candidate.
 */
public enum DiscoveryStatus {
    /** Surfaced by the screener but not yet evaluated by the LLM. */
    SCREENED,
    /** LLM evaluated the technicals and recommends presenting it to the user. */
    RECOMMENDED,
    /** LLM evaluated but does not recommend (kept for audit, hidden from UI). */
    NOT_RECOMMENDED,
    /** User accepted the recommendation — promoted to the watchlist. */
    PROMOTED,
    /** User rejected — dismissed; also added to the permanent blocklist. */
    DISMISSED
}
