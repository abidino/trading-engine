package com.trading.portfolio.domain.port.in;

import com.trading.portfolio.domain.model.PortfolioPosition;
import com.trading.portfolio.domain.model.PortfolioTransaction;
import com.trading.portfolio.domain.model.TransactionType;
import com.trading.shared.kernel.Money;
import com.trading.shared.kernel.Ticker;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Inbound port: all use cases exposed by the portfolio domain.
 */
public interface PortfolioUseCase {
    List<PortfolioPosition> listActivePositions();
    List<PortfolioPosition> listAllPositions();
    List<PortfolioTransaction> listTransactions(Ticker ticker);
    List<PortfolioTransaction> listAllTransactions();

    /** Best-effort refresh of live intraday quotes for all held tickers (page-load refresh). */
    void refreshHeldQuotes();

    /** Records a BUY or SELL transaction and updates the position accordingly. */
    PortfolioTransaction recordTransaction(
            Ticker ticker, TransactionType type,
            BigDecimal quantity, Money price, Money commission);

    void updateStopLoss(UUID positionId, Money stopLoss);

    /** Publishes AnalysisRequested event for ai-orchestration to pick up. */
    void requestAnalysis(Ticker ticker);
}
