package com.trading.portfolio.domain.port.out;

import com.trading.portfolio.domain.model.PortfolioTransaction;
import com.trading.shared.kernel.Ticker;

import java.util.List;

/** Outbound port: persistence for portfolio transactions. */
public interface PortfolioTransactionRepository {
    List<PortfolioTransaction> findByTicker(Ticker ticker);
    List<PortfolioTransaction> findAll();
    PortfolioTransaction save(PortfolioTransaction transaction);
}
