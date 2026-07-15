package com.trading.intelligence.web;

import com.trading.intelligence.domain.model.NewsArticle;

import java.util.List;

public record NewsResponse(
        String url,
        String headline,
        String source,
        String category,
        String summary,
        String publishedAt,
        List<TagDto> tags
) {
    public record TagDto(String ticker, String sentiment, String interpretation) {}

    public static NewsResponse from(NewsArticle a) {
        List<TagDto> tags = a.tags().stream()
                .map(t -> new TagDto(t.ticker(), t.sentiment().name(), t.interpretation()))
                .toList();
        return new NewsResponse(
                a.url(),
                a.headline(),
                a.source(),
                a.category() != null ? a.category().name() : null,
                a.summary(),
                a.publishedAt() != null ? a.publishedAt().toString() : null,
                tags
        );
    }
}
