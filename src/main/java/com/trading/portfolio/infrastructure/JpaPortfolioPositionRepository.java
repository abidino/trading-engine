package com.trading.portfolio.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaPortfolioPositionRepository extends JpaRepository<PortfolioPositionEntity, UUID> {
    Optional<PortfolioPositionEntity> findByTicker(String ticker);
    List<PortfolioPositionEntity> findByActiveTrue();
}
