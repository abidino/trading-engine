package com.trading.marketdata.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaPriceCandleRepository extends JpaRepository<PriceCandleEntity, UUID> {
    List<PriceCandleEntity> findByTicker(String ticker);
    List<PriceCandleEntity> findByTickerAndCandleDateBetween(String ticker, LocalDate from, LocalDate to);
    Optional<PriceCandleEntity> findByTickerAndCandleDate(String ticker, LocalDate date);

    @Query(value = """
            SELECT pc.*
            FROM (
                SELECT *,
                       ROW_NUMBER() OVER (
                           PARTITION BY ticker
                           ORDER BY candle_date DESC
                       ) AS rn
                FROM price_candles
                WHERE ticker IN (:tickers)
            ) pc
            WHERE pc.rn = 1
            """, nativeQuery = true)
    List<PriceCandleEntity> findLatestCandleForEachTicker(
            @Param("tickers") Collection<String> tickers
    );

}
