package com.trading.orchestration.domain.port.out;

import java.util.List;

/**
 * Outbound port: fetch social sentiment signals (Reddit posts, etc.) for a ticker.
 * Implemented in infrastructure by the RedditSocialSignalAdapter.
 */
public interface SocialSignalPort {
    /** Returns up to {@code limit} recent social media signals for the ticker. */
    List<String> fetchSignalsForTicker(String ticker, int limit);

    /**
     * Ensures reasonably fresh social data exists for the ticker before it is read,
     * gathering from providers if the local cache is missing or stale.
     * Default: no-op.
     */
    default void ensureFresh(String ticker) {}
}
