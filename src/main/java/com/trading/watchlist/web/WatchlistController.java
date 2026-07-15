package com.trading.watchlist.web;

import com.trading.shared.kernel.Money;
import com.trading.shared.kernel.Ticker;
import com.trading.watchlist.domain.model.WatchlistItem;
import com.trading.watchlist.domain.port.in.WatchlistUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/watchlist")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistUseCase watchlistUseCase;

    @GetMapping
    public ResponseEntity<List<WatchlistItemResponse>> listAll() {
        return ResponseEntity.ok(watchlistUseCase.listAll().stream()
                .map(WatchlistItemResponse::from).toList());
    }

    @PostMapping
    public ResponseEntity<WatchlistItemResponse> addItem(@Valid @RequestBody AddItemRequest dto) {
        WatchlistItem item = watchlistUseCase.addItem(new Ticker(dto.ticker()));
        return ResponseEntity.ok(WatchlistItemResponse.from(item));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable UUID id) {
        watchlistUseCase.removeItem(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/target-price")
    public ResponseEntity<Void> updateTargetPrice(@PathVariable UUID id, @RequestBody PriceRequest dto) {
        watchlistUseCase.updateTargetPrice(id, Money.of(dto.price()));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{ticker}/analyze")
    public ResponseEntity<Void> requestAnalysis(@PathVariable String ticker) {
        watchlistUseCase.requestAnalysis(new Ticker(ticker));
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable UUID id) {
        watchlistUseCase.approvePendingItem(id);
        return ResponseEntity.ok().build();
    }
}
