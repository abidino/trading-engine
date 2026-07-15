package com.trading.watchlist.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaWatchlistRepository extends JpaRepository<WatchlistItemEntity, UUID> {
    Optional<WatchlistItemEntity> findByTicker(String ticker);
}
