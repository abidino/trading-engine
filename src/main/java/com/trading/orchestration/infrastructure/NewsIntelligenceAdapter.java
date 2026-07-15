package com.trading.orchestration.infrastructure;

import com.trading.intelligence.domain.IntelligenceApplicationService;
import com.trading.intelligence.domain.model.NewsArticle;
import com.trading.intelligence.domain.model.NewsTag;
import com.trading.intelligence.domain.port.out.NewsRepository;
import com.trading.orchestration.domain.port.out.NewsPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Reads the last week's classified news for a ticker from the intelligence module,
 * including market-wide (macro / "ALL"-tagged) articles, and renders each into a
 * compact, sentiment-annotated line for the analysis prompt.
 *
 * Line format: {@code [yyyy-MM-dd | SOURCE | SCOPE/CATEGORY] headline — interpretation (SENTIMENT)}.
 */
@Slf4j
@Component
public class NewsIntelligenceAdapter implements NewsPort {

    private final NewsRepository newsRepository;
    private final IntelligenceApplicationService intelligenceService;
    private final int lookbackDays;
    private final Duration freshness;
    private final int scanLimit;

    public NewsIntelligenceAdapter(
            NewsRepository newsRepository,
            IntelligenceApplicationService intelligenceService,
            @Value("${orchestration.news.lookback-days:7}") int lookbackDays,
            @Value("${orchestration.freshness.news-minutes:30}") long freshnessMinutes,
            @Value("${orchestration.freshness.news-scan-limit:15}") int scanLimit) {
        this.newsRepository = newsRepository;
        this.intelligenceService = intelligenceService;
        this.lookbackDays = lookbackDays;
        this.freshness = Duration.ofMinutes(freshnessMinutes);
        this.scanLimit = scanLimit;
    }

    @Override
    public void ensureFresh(String ticker) {
        Instant since = Instant.now().minus(lookbackDays, ChronoUnit.DAYS);
        Instant newest = newsRepository.findForTickerSince(ticker, since).stream()
                .map(NewsArticle::capturedAt)
                .filter(java.util.Objects::nonNull)
                .max(Instant::compareTo)
                .orElse(null);
        if (newest != null && newest.isAfter(Instant.now().minus(freshness))) {
            log.debug("News for {} fresh (captured {}), skipping scan", ticker, newest);
            return;
        }
        try {
            int saved = intelligenceService.scanTickerNews(ticker, scanLimit);
            log.info("News readiness: scanned {} — {} new articles", ticker, saved);
        } catch (Exception e) {
            log.warn("News readiness scan failed for {}: {}", ticker, e.getMessage());
        }
    }

    @Override
    public List<String> fetchHeadlinesForTicker(String ticker, int limit) {
        Instant since = Instant.now().minus(lookbackDays, ChronoUnit.DAYS);
        return newsRepository.findForTickerSince(ticker, since).stream()
                .limit(Math.max(1, limit))
                .map(article -> render(article, ticker))
                .toList();
    }

    /** Render a single article, preferring the tag relevant to this ticker. */
    private String render(NewsArticle article, String ticker) {
        NewsTag tag = article.tags().stream()
                .filter(t -> t.ticker().equalsIgnoreCase(ticker))
                .findFirst()
                .orElseGet(() -> article.tags().stream()
                        .filter(NewsTag::isMarketWide)
                        .findFirst()
                        .orElse(article.tags().isEmpty() ? null : article.tags().get(0)));

        String date = article.publishedAt() == null ? "n/a"
                : article.publishedAt().atZone(ZoneOffset.UTC).toLocalDate().toString();
        String scope = (tag != null && tag.isMarketWide()) ? "MACRO" : ticker.toUpperCase();
        String header = "[%s | %s | %s/%s]".formatted(date, article.source(), scope, article.category());

        if (tag != null && tag.interpretation() != null && !tag.interpretation().isBlank()) {
            return "%s %s — %s (%s)".formatted(header, article.headline(),
                    tag.interpretation(), tag.sentiment());
        }
        return "%s %s".formatted(header, article.headline());
    }
}
