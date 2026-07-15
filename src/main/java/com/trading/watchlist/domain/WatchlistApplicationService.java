package com.trading.watchlist.domain;

import com.trading.shared.kernel.AnalysisRequestType;
import com.trading.shared.kernel.Money;
import com.trading.shared.kernel.Ticker;
import com.trading.shared.kernel.TradingLevels;
import com.trading.shared.kernel.event.AddToWatchlistRecommended;
import com.trading.shared.kernel.event.AnalysisRequested;
import com.trading.shared.kernel.event.DecisionProduced;
import com.trading.watchlist.domain.model.WatchlistItem;
import com.trading.watchlist.domain.port.in.WatchlistUseCase;
import com.trading.watchlist.domain.port.out.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class WatchlistApplicationService implements WatchlistUseCase {

    private final WatchlistRepository watchlistRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<WatchlistItem> listAll() {
        return watchlistRepository.findAll();
    }

    @Override
    public WatchlistItem addItem(Ticker ticker) {
        WatchlistItem saved = watchlistRepository.save(WatchlistItem.create(ticker));
        // Manual add → analyse immediately so buyable levels appear without waiting for the sweep.
        eventPublisher.publishEvent(AnalysisRequested.of(ticker, AnalysisRequestType.WATCHLIST_REVIEW));
        log.info("Watchlist item added and analysis requested: {}", ticker);
        return saved;
    }

    @Override
    public void removeItem(UUID id) {
        watchlistRepository.deleteById(id);
    }

    @Override
    public void updateTargetPrice(UUID id, Money targetPrice) {
        watchlistRepository.findById(id).ifPresent(item -> {
            item.setTargetPrice(targetPrice);
            watchlistRepository.save(item);
        });
    }

    @Override
    public void updateStopLoss(UUID id, Money stopLoss) {
        watchlistRepository.findById(id).ifPresent(item -> {
            item.setStopLoss(stopLoss);
            watchlistRepository.save(item);
        });
    }

    @Override
    public void requestAnalysis(Ticker ticker) {
        eventPublisher.publishEvent(AnalysisRequested.of(ticker, AnalysisRequestType.WATCHLIST_REVIEW));
    }

    @Override
    public void approvePendingItem(UUID id) {
        watchlistRepository.findById(id).ifPresent(item -> {
            item.approve();
            watchlistRepository.save(item);
            // Approving a discovery draft → analyse now to produce actionable levels.
            eventPublisher.publishEvent(
                    AnalysisRequested.of(item.getTicker(), AnalysisRequestType.WATCHLIST_REVIEW));
            log.info("Watchlist item approved and analysis requested: {}", item.getTicker());
        });
    }

    /** Consumes AddToWatchlistRecommended from discovery → ai-orchestration pipeline. */
    @EventListener
    public void onAddToWatchlistRecommended(AddToWatchlistRecommended event) {
        boolean alreadyExists = watchlistRepository.findByTicker(event.ticker()).isPresent();
        if (!alreadyExists) {
            WatchlistItem draft = WatchlistItem.createFromRecommendation(event.ticker(), event.reasoning());
            watchlistRepository.save(draft);
            log.info("Draft watchlist item created from recommendation: {}", event.ticker());
        }
    }

    /**
     * Consumes DecisionProduced and records the analysis's take-profit / stop-loss onto the
     * matching watchlist item so the UI shows the current buyable target and protective stop.
     */
    @EventListener
    public void onDecisionProduced(DecisionProduced event) {
        if (event.requestType() != AnalysisRequestType.WATCHLIST_REVIEW) {
            return;
        }
        TradingLevels levels = event.levels();
        if (levels == null || !levels.hasAnyLevel()) {
            return;
        }
        watchlistRepository.findByTicker(event.ticker()).ifPresent(item -> {
            if (levels.takeProfit() != null) {
                item.setTargetPrice(Money.of(BigDecimal.valueOf(levels.takeProfit())));
            }
            if (levels.stopLoss() != null) {
                item.setStopLoss(Money.of(BigDecimal.valueOf(levels.stopLoss())));
            }
            watchlistRepository.save(item);
            log.info("Watchlist item {} updated with analysis levels (target={}, stop={})",
                    event.ticker(), levels.takeProfit(), levels.stopLoss());
        });
    }
}
