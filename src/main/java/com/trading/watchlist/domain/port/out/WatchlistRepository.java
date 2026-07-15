package com.trading.watchlist.domain.port.out;

import com.trading.shared.kernel.Ticker;
import com.trading.watchlist.domain.model.WatchlistItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Outbound port: watchlist persistence. */
public interface WatchlistRepository {
    List<WatchlistItem> findAll();
    Optional<WatchlistItem> findById(UUID id);
    Optional<WatchlistItem> findByTicker(Ticker ticker);
    WatchlistItem save(WatchlistItem item);
    void deleteById(UUID id);
}
