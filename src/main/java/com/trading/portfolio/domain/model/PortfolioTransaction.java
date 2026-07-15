package com.trading.portfolio.domain.model;

import com.trading.shared.kernel.Money;
import com.trading.shared.kernel.Ticker;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Value Object: an immutable buy/sell transaction record.
 */
public record PortfolioTransaction(
        UUID id,
        Ticker ticker,
        TransactionType transactionType,
        BigDecimal quantity,
        Money price,
        Money commission,
        Instant executedAt
) {
    public static PortfolioTransaction create(
            Ticker ticker, TransactionType type,
            BigDecimal quantity, Money price, Money commission) {
        return new PortfolioTransaction(
                UUID.randomUUID(), ticker, type, quantity, price, commission, Instant.now());
    }

    /** Gross transaction value (quantity × price), not including commission. */
    public Money grossValue() {
        return price.multiply(quantity);
    }

    /** Net transaction value: grossValue + commission (total cash outflow/inflow). */
    public Money netValue() {
        return grossValue().add(commission);
    }
}
