package com.trading.portfolio.domain.port.out;

import com.trading.portfolio.domain.model.PortfolioPosition;
import com.trading.shared.kernel.Ticker;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Outbound port: persistence for portfolio positions. */
public interface PortfolioPositionRepository {
    List<PortfolioPosition> findAll();
    List<PortfolioPosition> findAllActive();
    Optional<PortfolioPosition> findById(UUID id);
    Optional<PortfolioPosition> findByTicker(Ticker ticker);
    PortfolioPosition save(PortfolioPosition position);
}
