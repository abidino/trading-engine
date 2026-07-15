package com.trading.intelligence.domain.model;

import java.time.Instant;

/**
 * Value Object: an article as returned by an external news provider, before
 * categorisation / sentiment analysis. Carries the raw text only transiently so
 * the LLM can read it — it is never persisted.
 */
public record RawNewsArticle(
        String headline,
        String source,
        String url,
        String content,
        Instant publishedAt
) {}
