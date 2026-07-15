package com.trading.marketdata.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaIntradayQuoteRepository extends JpaRepository<IntradayQuoteEntity, UUID> {

    Optional<IntradayQuoteEntity> findFirstByTickerOrderByCapturedAtDesc(String ticker);

    List<IntradayQuoteEntity> findByTickerAndCapturedAtGreaterThanEqualOrderByCapturedAtDesc(
            String ticker, Instant since);
}
