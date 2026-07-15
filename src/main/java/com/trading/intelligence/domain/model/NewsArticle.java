package com.trading.intelligence.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate: a persisted news article.
 *
 * Stores only the link + a short summary (full article text is never kept), the
 * category, and one or more {@link NewsTag}s describing which tickers (or the
 * whole market via {@code ALL}) the article relates to and how it reads for each.
 *
 * The {@code url} is the natural unique key — re-scanning the same article is a
 * no-op.
 */
public record NewsArticle(
        UUID id,
        String url,
        String headline,
        String source,
        NewsCategory category,
        String summary,
        Instant publishedAt,
        Instant capturedAt,
        List<NewsTag> tags
) {
    public NewsArticle {
        tags = tags != null ? List.copyOf(tags) : List.of();
    }

    public static NewsArticle create(String url, String headline, String source,
                                     NewsCategory category, String summary,
                                     Instant publishedAt, List<NewsTag> tags) {
        return new NewsArticle(UUID.randomUUID(), url, headline, source, category,
                summary, publishedAt, Instant.now(), tags);
    }
}
