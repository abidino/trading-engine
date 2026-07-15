package com.trading.intelligence.infrastructure;

import com.trading.intelligence.domain.model.NewsArticle;
import com.trading.intelligence.domain.model.NewsTag;
import com.trading.intelligence.domain.port.out.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NewsRepositoryAdapter implements NewsRepository {

    private final JpaNewsArticleRepository jpa;

    @Override
    public NewsArticle save(NewsArticle article) {
        return toDomain(jpa.save(toEntity(article)));
    }

    @Override
    public Optional<NewsArticle> findByUrl(String url) {
        return jpa.findByUrl(url).map(this::toDomain);
    }

    @Override
    public List<NewsArticle> findForTickerSince(String ticker, Instant since) {
        return jpa.findForTickerSince(ticker, since).stream().map(this::toDomain).toList();
    }

    @Override
    public List<NewsArticle> findRecent(int limit) {
        return jpa.findAllByOrderByPublishedAtDesc(PageRequest.of(0, Math.max(1, limit)))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<NewsArticle> findByTicker(String ticker, int limit) {
        return jpa.findForTicker(ticker, PageRequest.of(0, Math.max(1, limit)))
                .stream().map(this::toDomain).toList();
    }

    // -----------------------------------------------------------------------

    private NewsArticleEntity toEntity(NewsArticle a) {
        NewsArticleEntity entity = NewsArticleEntity.builder()
                .id(a.id())
                .url(a.url())
                .headline(a.headline())
                .source(a.source())
                .category(a.category())
                .summary(a.summary())
                .publishedAt(a.publishedAt())
                .capturedAt(a.capturedAt())
                .build();
        List<NewsTagEntity> tagEntities = a.tags().stream()
                .map(t -> NewsTagEntity.builder()
                        .id(UUID.randomUUID())
                        .article(entity)
                        .ticker(t.ticker())
                        .sentiment(t.sentiment())
                        .interpretation(t.interpretation())
                        .build())
                .toList();
        entity.getTags().addAll(tagEntities);
        return entity;
    }

    private NewsArticle toDomain(NewsArticleEntity e) {
        List<NewsTag> tags = e.getTags().stream()
                .map(t -> new NewsTag(t.getTicker(), t.getSentiment(), t.getInterpretation()))
                .toList();
        return new NewsArticle(
                e.getId(), e.getUrl(), e.getHeadline(), e.getSource(),
                e.getCategory(), e.getSummary(), e.getPublishedAt(), e.getCapturedAt(), tags);
    }
}
