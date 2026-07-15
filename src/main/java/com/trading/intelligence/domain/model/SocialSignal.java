package com.trading.intelligence.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity: a raw social media signal captured for a ticker (Reddit post, tweet, etc.).
 */
public record SocialSignal(
        UUID id,
        String ticker,
        String source,
        String content,
        double engagementScore,
        double sentimentScore,
        Instant capturedAt
) {
    public static SocialSignal create(String ticker, String source, String content,
                                      double engagement, double sentiment) {
        return new SocialSignal(UUID.randomUUID(), ticker, source, content,
                engagement, sentiment, Instant.now());
    }
}
