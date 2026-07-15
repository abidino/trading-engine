package com.trading.intelligence.domain.port.out;

import com.trading.intelligence.domain.model.NewsArticle;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Outbound port: local news cache persistence. */
public interface NewsRepository {

    NewsArticle save(NewsArticle article);

    /** Uniqueness check — the article URL is the natural key. */
    Optional<NewsArticle> findByUrl(String url);

    /**
     * Articles relevant to a ticker published on/after {@code since}, newest first.
     * Includes market-wide ({@code ALL}) macro news.
     */
    List<NewsArticle> findForTickerSince(String ticker, Instant since);

    /** Most recent articles across all tickers, newest first. */
    List<NewsArticle> findRecent(int limit);

    /** Most recent articles for a ticker (incl. {@code ALL}), newest first, capped. */
    List<NewsArticle> findByTicker(String ticker, int limit);
}
