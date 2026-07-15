package com.trading.notification.domain;

import com.trading.decisionlog.domain.model.DecisionRecord;
import com.trading.decisionlog.domain.port.out.DecisionRecordRepository;
import com.trading.marketdata.domain.model.IntradayQuote;
import com.trading.marketdata.domain.model.MarketSession;
import com.trading.marketdata.domain.port.in.IntradayQuoteUseCase;
import com.trading.notification.domain.AlertEvaluationService.AlertCandidate;
import com.trading.notification.domain.model.AlertType;
import com.trading.notification.domain.port.out.AlertLogRepository;
import com.trading.notification.domain.port.out.NotificationDeliveryPort;
import com.trading.notification.domain.port.out.TriggeredAlertRepository;
import com.trading.portfolio.domain.port.in.PortfolioUseCase;
import com.trading.shared.kernel.AnalysisRequestType;
import com.trading.shared.kernel.Ticker;
import com.trading.shared.kernel.TradingLevels;
import com.trading.watchlist.domain.model.WatchlistItem;
import com.trading.watchlist.domain.port.in.WatchlistUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers proactive threshold alerts: the pure level-crossing rules and the once-per-day
 * de-duplication that prevents alert spam.
 */
@ExtendWith(MockitoExtension.class)
class AlertEvaluationTest {

    @Mock PortfolioUseCase portfolioUseCase;
    @Mock WatchlistUseCase watchlistUseCase;
    @Mock DecisionRecordRepository decisionRepository;
    @Mock IntradayQuoteUseCase intradayQuotes;
    @Mock NotificationDeliveryPort emailDelivery;
    @Mock TriggeredAlertRepository triggeredAlerts;
    @Mock AlertLogRepository alertLogRepository;

    @InjectMocks AlertEvaluationService service;

    private static TradingLevels levels(Double entryHigh, Double idealEntry, Double stopLoss, Double takeProfit) {
        return new TradingLevels(null, entryHigh, null, idealEntry, null, stopLoss, takeProfit, null, null);
    }

    // ---- Pure rule evaluation ------------------------------------------------

    @Test
    void stopLossBreachProducesStopAlert() {
        var out = AlertEvaluationService.evaluateTicker("AAA", 90.0, "BUY",
                levels(100.0, 100.0, 95.0, 130.0));
        assertThat(out).extracting(AlertCandidate::type).contains(AlertType.STOP_LOSS);
    }

    @Test
    void reachingTakeProfitProducesTargetAlert() {
        var out = AlertEvaluationService.evaluateTicker("AAA", 135.0, "BUY",
                levels(100.0, 100.0, 95.0, 130.0));
        assertThat(out).extracting(AlertCandidate::type).contains(AlertType.TAKE_PROFIT);
    }

    @Test
    void priceInBuyZoneWithActionableDecisionProducesEntryAlert() {
        var out = AlertEvaluationService.evaluateTicker("AAA", 99.0, "BUY",
                levels(100.0, 100.0, 90.0, 130.0));
        assertThat(out).extracting(AlertCandidate::type).contains(AlertType.ENTRY_ZONE);
    }

    @Test
    void noEntryAlertWhenActionIsNotActionable() {
        var out = AlertEvaluationService.evaluateTicker("AAA", 99.0, "HOLD",
                levels(100.0, 100.0, 90.0, 130.0));
        assertThat(out).extracting(AlertCandidate::type).doesNotContain(AlertType.ENTRY_ZONE);
    }

    @Test
    void noEntryAlertWhenPriceBrokeBelowStop() {
        // Price 89 is below stop 90 → treated as stop-loss, not an entry opportunity.
        var out = AlertEvaluationService.evaluateTicker("AAA", 89.0, "BUY",
                levels(100.0, 100.0, 90.0, 130.0));
        assertThat(out).extracting(AlertCandidate::type)
                .contains(AlertType.STOP_LOSS)
                .doesNotContain(AlertType.ENTRY_ZONE);
    }

    @Test
    void noAlertsWhenNoLevels() {
        var out = AlertEvaluationService.evaluateTicker("AAA", 99.0, "BUY", TradingLevels.empty());
        assertThat(out).isEmpty();
    }

    // ---- Dispatch + de-duplication ------------------------------------------

    @Test
    void firesAlertAndPersistsWhenNotSeenToday() {
        stubOneWatchlistTicker("AAA", 90.0, "BUY", levels(100.0, 100.0, 95.0, 130.0));
        when(triggeredAlerts.existsForDay(eq("AAA"), any(), any())).thenReturn(false);

        int fired = service.evaluateAll();

        assertThat(fired).isEqualTo(1);
        verify(emailDelivery, times(1)).deliverMessage(any(), any());
        verify(triggeredAlerts, times(1)).save(any());
    }

    @Test
    void cooldownPreventsDuplicateSameDay() {
        stubOneWatchlistTicker("AAA", 90.0, "BUY", levels(100.0, 100.0, 95.0, 130.0));
        when(triggeredAlerts.existsForDay(eq("AAA"), any(), any())).thenReturn(true);

        int fired = service.evaluateAll();

        assertThat(fired).isZero();
        verify(emailDelivery, never()).deliverMessage(any(), any());
        verify(triggeredAlerts, never()).save(any());
    }

    private void stubOneWatchlistTicker(String ticker, double price, String action, TradingLevels levels) {
        lenient().when(portfolioUseCase.listActivePositions()).thenReturn(List.of());
        when(watchlistUseCase.listAll()).thenReturn(List.of(WatchlistItem.create(new Ticker(ticker))));
        IntradayQuote quote = IntradayQuote.create(ticker, MarketSession.REGULAR,
                BigDecimal.valueOf(price), BigDecimal.valueOf(price + 10), 1_000L, Instant.now());
        when(intradayQuotes.latestQuote(ticker)).thenReturn(Optional.of(quote));
        DecisionRecord record = new DecisionRecord(UUID.randomUUID(), new Ticker(ticker),
                AnalysisRequestType.WATCHLIST_REVIEW, action, 0.8, "reason",
                "tech", "fund", "news", "social", "", List.of(), levels, Instant.now());
        when(decisionRepository.findLatestByTicker(new Ticker(ticker))).thenReturn(Optional.of(record));
    }
}
