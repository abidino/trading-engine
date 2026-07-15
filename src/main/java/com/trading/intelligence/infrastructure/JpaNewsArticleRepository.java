package com.trading.intelligence.infrastructure;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaNewsArticleRepository extends JpaRepository<NewsArticleEntity, UUID> {

    Optional<NewsArticleEntity> findByUrl(String url);

    List<NewsArticleEntity> findAllByOrderByPublishedAtDesc(Pageable pageable);

    @Query("""
            select distinct a from NewsArticleEntity a
            join a.tags t
            where (t.ticker = :ticker or t.ticker = 'ALL')
              and a.publishedAt >= :since
            order by a.publishedAt desc
            """)
    List<NewsArticleEntity> findForTickerSince(@Param("ticker") String ticker,
                                               @Param("since") Instant since);

    @Query("""
            select distinct a from NewsArticleEntity a
            join a.tags t
            where (t.ticker = :ticker or t.ticker = 'ALL')
            order by a.publishedAt desc
            """)
    List<NewsArticleEntity> findForTicker(@Param("ticker") String ticker, Pageable pageable);
}
