package com.trading.intelligence.domain.model;

/**
 * Value Object: links a news article to a ticker (or the {@link #ALL} sentinel
 * for market-wide macro news) together with the LLM's directional reading and a
 * short interpretation of what the news means FOR that ticker.
 *
 * The interpretation replaces a generic summary — per the product requirement,
 * we store "is this good or bad for this stock and why" rather than a neutral
 * recap.
 */
public record NewsTag(
        String ticker,
        NewsSentiment sentiment,
        String interpretation
) {
    /** Sentinel ticker for macro news that concerns the whole market. */
    public static final String ALL = "ALL";

    public static NewsTag of(String ticker, NewsSentiment sentiment, String interpretation) {
        return new NewsTag(ticker == null ? ALL : ticker.toUpperCase(), sentiment, interpretation);
    }

    public boolean isMarketWide() {
        return ALL.equals(ticker);
    }
}
