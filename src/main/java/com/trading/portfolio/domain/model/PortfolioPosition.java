package com.trading.portfolio.domain.model;

import com.trading.shared.kernel.Money;
import com.trading.shared.kernel.Ticker;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate Root: a single portfolio position for a ticker.
 * Mutable state (quantity, stop-loss) changes only through explicit domain methods.
 */
public class PortfolioPosition {

    private final UUID id;
    private final Ticker ticker;
    private BigDecimal quantity;
    private Money averageEntryPrice;
    private Money stopLossLevel;
    private boolean active;
    private Money currentMarketPrice;

    public PortfolioPosition(UUID id, Ticker ticker, BigDecimal quantity, Money averageEntryPrice) {
        this.id = Objects.requireNonNull(id);
        this.ticker = Objects.requireNonNull(ticker);
        this.quantity = Objects.requireNonNull(quantity);
        this.averageEntryPrice = Objects.requireNonNull(averageEntryPrice);
        this.active = quantity.compareTo(BigDecimal.ZERO) > 0;
    }

    // -----------------------------------------------------------------------
    // Domain behaviours
    // -----------------------------------------------------------------------

    /**
     * Adjusts position after a SELL transaction.
     * Marks position as closed (inactive) when quantity reaches zero.
     */
    public void recordSell(BigDecimal soldQuantity) {
        Objects.requireNonNull(soldQuantity);
        if (soldQuantity.compareTo(this.quantity) > 0) {
            throw new IllegalStateException(
                    "Cannot sell " + soldQuantity + " of " + ticker + " — only " + quantity + " held.");
        }
        this.quantity = this.quantity.subtract(soldQuantity);
        this.active = this.quantity.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Updates the average entry price after a BUY (called by PortfolioDomainService).
     */
    public void updateAfterBuy(BigDecimal addedQuantity, Money newAverage) {
        this.quantity = this.quantity.add(addedQuantity);
        this.averageEntryPrice = Objects.requireNonNull(newAverage);
        this.active = true;
    }

    public void updateStopLoss(Money stopLoss) {
        this.stopLossLevel = Objects.requireNonNull(stopLoss);
    }

    public void updateCurrentMarketPrice(Money marketPrice) {
        this.currentMarketPrice = Objects.requireNonNull(marketPrice);
    }

    // -----------------------------------------------------------------------
    // Derived valuation (null-safe: falls back to entry price when no live quote)
    // -----------------------------------------------------------------------

    /** Live price if available and positive, otherwise the average entry price. */
    public Money effectiveCurrentPrice() {
        return (currentMarketPrice != null && currentMarketPrice.isPositive())
                ? currentMarketPrice
                : averageEntryPrice;
    }

    public BigDecimal marketValue() {
        return quantity.multiply(effectiveCurrentPrice().amount());
    }

    public BigDecimal costBasis() {
        return quantity.multiply(averageEntryPrice.amount());
    }

    public BigDecimal unrealizedPnl() {
        return marketValue().subtract(costBasis());
    }

    public BigDecimal unrealizedPnlPercent() {
        BigDecimal cost = costBasis();
        return cost.signum() != 0
                ? unrealizedPnl().divide(cost, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
    }
    // -----------------------------------------------------------------------
    // Getters
    // -----------------------------------------------------------------------

    public UUID getId() { return id; }
    public Ticker getTicker() { return ticker; }
    public BigDecimal getQuantity() { return quantity; }
    public Money getAverageEntryPrice() { return averageEntryPrice; }
    public Money getStopLossLevel() { return stopLossLevel; }
    public boolean isActive() { return active; }
    public Money getCurrentMarketPrice() { return currentMarketPrice; }
}
