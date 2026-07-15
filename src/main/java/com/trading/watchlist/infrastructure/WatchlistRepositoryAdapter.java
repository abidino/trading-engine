package com.trading.watchlist.infrastructure;

import com.trading.shared.kernel.Money;
import com.trading.shared.kernel.Ticker;
import com.trading.watchlist.domain.model.WatchlistItem;
import com.trading.watchlist.domain.port.out.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WatchlistRepositoryAdapter implements WatchlistRepository {

    private final JpaWatchlistRepository jpa;

    @Override
    public List<WatchlistItem> findAll() {
        return jpa.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<WatchlistItem> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<WatchlistItem> findByTicker(Ticker ticker) {
        return jpa.findByTicker(ticker.value()).map(this::toDomain);
    }

    @Override
    public WatchlistItem save(WatchlistItem item) {
        return toDomain(jpa.save(toEntity(item)));
    }

    @Override
    public void deleteById(UUID id) {
        jpa.deleteById(id);
    }

    private WatchlistItem toDomain(WatchlistItemEntity e) {
        WatchlistItem item = new WatchlistItem(e.getId(), new Ticker(e.getTicker()), e.getAddedAt());
        if (e.getTargetPrice() != null) item.setTargetPrice(Money.of(e.getTargetPrice()));
        if (e.getStopLoss() != null) item.setStopLoss(Money.of(e.getStopLoss()));
        item.setNotes(e.getNotes());
        if (e.isApproved()) item.approve();
        return item;
    }

    private WatchlistItemEntity toEntity(WatchlistItem i) {
        return WatchlistItemEntity.builder()
                .id(i.getId()).ticker(i.getTicker().value()).addedAt(i.getAddedAt())
                .targetPrice(i.getTargetPrice() != null ? i.getTargetPrice().amount() : null)
                .stopLoss(i.getStopLoss() != null ? i.getStopLoss().amount() : null)
                .notes(i.getNotes()).approved(i.isApproved())
                .build();
    }
}
