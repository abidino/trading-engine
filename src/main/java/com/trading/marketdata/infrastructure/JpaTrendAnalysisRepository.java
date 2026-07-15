package com.trading.marketdata.infrastructure;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaTrendAnalysisRepository extends JpaRepository<TrendAnalysisEntity, UUID> {

    Optional<TrendAnalysisEntity> findFirstByTickerOrderByAnalysisDateDesc(String ticker);

    List<TrendAnalysisEntity> findByTickerOrderByAnalysisDateDesc(String ticker, Pageable pageable);

    List<TrendAnalysisEntity> findByTickerAndAnalysisDateBetweenOrderByAnalysisDateDesc(
            String ticker, LocalDate from, LocalDate to);

    Optional<TrendAnalysisEntity> findByTickerAndAnalysisDate(String ticker, LocalDate analysisDate);
}
