package com.trading.portfolio.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaPortfolioTransactionRepository extends JpaRepository<PortfolioTransactionEntity, UUID> {
    List<PortfolioTransactionEntity> findByTicker(String ticker);
}
