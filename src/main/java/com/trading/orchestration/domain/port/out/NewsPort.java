package com.trading.orchestration.domain.port.out;

import java.util.List;

/**
 * Outbound port: fetch recent news headlines for a ticker.
 * Implemented in infrastructure by the NewsApiAdapter (or local DB cache).
 */
public interface NewsPort {
    /** Returns up to {@code limit} recent news headlines/snippets for the ticker. */
    List<String> fetchHeadlinesForTicker(String ticker, int limit);

    /**
     * Ensures reasonably fresh data exists for the ticker before it is read,
     * gathering from providers if the local cache is missing or stale.
     * Default: no-op (adapters that can gather override this).
     */
    default void ensureFresh(String ticker) {}
}
