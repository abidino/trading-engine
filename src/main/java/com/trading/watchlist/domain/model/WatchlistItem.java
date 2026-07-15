package com.trading.watchlist.domain.model;

import com.trading.shared.kernel.Money;
import com.trading.shared.kernel.Ticker;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate Root: a stock the user is actively monitoring as a buy candidate.
 */
public class WatchlistItem {

    private final UUID id;
    private final Ticker ticker;
    private final Instant addedAt;
    private Money targetPrice;
    private Money stopLoss;
    private String notes;
    private boolean approved; // false = pending recommendation review

    public WatchlistItem(UUID id, Ticker ticker, Instant addedAt) {
        this.id = Objects.requireNonNull(id);
        this.ticker = Objects.requireNonNull(ticker);
        this.addedAt = Objects.requireNonNull(addedAt);
        this.approved = true;
    }

    public static WatchlistItem create(Ticker ticker) {
        return new WatchlistItem(UUID.randomUUID(), ticker, Instant.now());
    }

    /** Creates a draft item from a discovery recommendation — pending user approval. */
    public static WatchlistItem createFromRecommendation(Ticker ticker, String reasoning) {
        WatchlistItem item = new WatchlistItem(UUID.randomUUID(), ticker, Instant.now());
        item.notes = "Recommended by AI: " + reasoning;
        item.approved = false;
        return item;
    }

    public void approve() { this.approved = true; }
    public void setTargetPrice(Money price) { this.targetPrice = price; }
    public void setStopLoss(Money stopLoss) { this.stopLoss = stopLoss; }
    public void setNotes(String notes) { this.notes = notes; }

    public UUID getId() { return id; }
    public Ticker getTicker() { return ticker; }
    public Instant getAddedAt() { return addedAt; }
    public Money getTargetPrice() { return targetPrice; }
    public Money getStopLoss() { return stopLoss; }
    public String getNotes() { return notes; }
    public boolean isApproved() { return approved; }
}
