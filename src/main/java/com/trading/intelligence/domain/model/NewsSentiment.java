package com.trading.intelligence.domain.model;

/**
 * Directional reading of a news item with respect to a tagged ticker (or the
 * market as a whole when tagged {@code ALL}).
 */
public enum NewsSentiment {
    POSITIVE,
    NEGATIVE,
    NEUTRAL
}
