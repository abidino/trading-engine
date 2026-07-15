package com.trading.watchlist.domain.port.in;

import com.trading.shared.kernel.Money;
import com.trading.shared.kernel.Ticker;
import com.trading.watchlist.domain.model.WatchlistItem;

import java.util.List;
import java.util.UUID;

/** Inbound port: all use cases of the watchlist domain. */
public interface WatchlistUseCase {
    List<WatchlistItem> listAll();
    WatchlistItem addItem(Ticker ticker);
    void removeItem(UUID id);
    void updateTargetPrice(UUID id, Money targetPrice);
    void updateStopLoss(UUID id, Money stopLoss);
    void requestAnalysis(Ticker ticker);
    /** Approve a pending recommendation from discovery. */
    void approvePendingItem(UUID id);
}
