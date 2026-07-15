package com.trading.intelligence.domain.model;

/**
 * Category of a news article — separates macro market-movers from
 * sector/stock-specific coverage.
 */
public enum NewsCategory {
    /** Federal Reserve / monetary policy (rates, FOMC, Fed chair remarks). */
    MACRO_FED,
    /** Political / government / presidential statements affecting markets. */
    MACRO_POLITICAL,
    /** Broad economic data (inflation, jobs, GDP, trade). */
    MACRO_ECONOMIC,
    /** Sector- or industry-wide news. */
    SECTOR,
    /** Company-specific news tied to one ticker. */
    STOCK,
    /** Anything that doesn't clearly fit the above. */
    OTHER
}
