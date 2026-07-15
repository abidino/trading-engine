package com.trading.portfolio.domain.service;

import com.trading.portfolio.domain.model.PortfolioPosition;
import com.trading.portfolio.domain.model.PortfolioTransaction;
import com.trading.portfolio.domain.model.TransactionType;
import com.trading.portfolio.domain.port.out.PortfolioPositionRepository;
import com.trading.portfolio.domain.port.out.PortfolioTransactionRepository;
import com.trading.shared.kernel.Money;
import com.trading.shared.kernel.Ticker;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Domain service: pure portfolio business logic.
 * Knows how to record transactions and recalculate average entry prices.
 * No Spring, no IO.
 */
public class PortfolioDomainService {

    private final PortfolioPositionRepository positionRepo;
    private final PortfolioTransactionRepository transactionRepo;

    public PortfolioDomainService(
            PortfolioPositionRepository positionRepo,
            PortfolioTransactionRepository transactionRepo) {
        this.positionRepo = positionRepo;
        this.transactionRepo = transactionRepo;
    }

    public PortfolioTransaction recordTransaction(
            Ticker ticker, TransactionType type,
            BigDecimal quantity, Money price, Money commission) {

        PortfolioTransaction tx = PortfolioTransaction.create(ticker, type, quantity, price, commission);
        transactionRepo.save(tx);

        if (type == TransactionType.BUY) {
            positionRepo.findByTicker(ticker)
                    .ifPresentOrElse(
                            existing -> {
                                Money newAvg = recalculateAverage(
                                        existing.getQuantity(), existing.getAverageEntryPrice(),
                                        quantity, price);
                                existing.updateAfterBuy(quantity, newAvg);
                                positionRepo.save(existing);
                            },
                            () -> positionRepo.save(
                                    new PortfolioPosition(UUID.randomUUID(), ticker, quantity, price))
                    );
        } else {
            positionRepo.findByTicker(ticker).ifPresent(pos -> {
                pos.recordSell(quantity);
                positionRepo.save(pos);
            });
        }
        return tx;
    }

    private Money recalculateAverage(
            BigDecimal existingQty, Money existingAvg,
            BigDecimal addedQty, Money addedPrice) {
        BigDecimal totalQty = existingQty.add(addedQty);
        BigDecimal totalCost = existingAvg.amount().multiply(existingQty)
                .add(addedPrice.amount().multiply(addedQty));
        return Money.of(totalCost.divide(totalQty, 4, RoundingMode.HALF_UP));
    }
}
