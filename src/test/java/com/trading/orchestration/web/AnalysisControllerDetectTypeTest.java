package com.trading.orchestration.web;

import com.trading.orchestration.domain.AnalysisRunStore;
import com.trading.orchestration.domain.port.in.RequestAnalysisUseCase;
import com.trading.portfolio.domain.model.PortfolioPosition;
import com.trading.portfolio.domain.port.in.PortfolioUseCase;
import com.trading.shared.kernel.Money;
import com.trading.shared.kernel.Ticker;
import com.trading.watchlist.domain.model.WatchlistItem;
import com.trading.watchlist.domain.port.in.WatchlistUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Verifies the manual-trigger intent auto-detection: a ticker is classified by where it
 * currently lives (open portfolio position → SELL/HOLD review; watchlist → BUY/WAIT review;
 * otherwise a fresh discovery candidate).
 */
@ExtendWith(MockitoExtension.class)
class AnalysisControllerDetectTypeTest {

    @Mock RequestAnalysisUseCase requestAnalysisUseCase;
    @Mock AnalysisRunStore runStore;
    @Mock PortfolioUseCase portfolioUseCase;
    @Mock WatchlistUseCase watchlistUseCase;

    private AnalysisController controller() {
        return new AnalysisController(requestAnalysisUseCase, runStore, portfolioUseCase, watchlistUseCase);
    }

    private PortfolioPosition position(String ticker) {
        return new PortfolioPosition(UUID.randomUUID(), new Ticker(ticker),
                BigDecimal.ONE, Money.of(100.0));
    }

    private WatchlistItem watchItem(String ticker) {
        return WatchlistItem.create(new Ticker(ticker));
    }

    @Test
    void portfolioPositionYieldsPortfolioReview() {
        when(portfolioUseCase.listActivePositions()).thenReturn(List.of(position("NVDA")));
        lenient().when(watchlistUseCase.listAll()).thenReturn(List.of());

        var body = controller().suggestType("NVDA").getBody();

        assertThat(body).containsEntry("requestType", "PORTFOLIO_REVIEW");
    }

    @Test
    void watchlistEntryYieldsWatchlistReview() {
        when(portfolioUseCase.listActivePositions()).thenReturn(List.of());
        when(watchlistUseCase.listAll()).thenReturn(List.of(watchItem("AAPL")));

        var body = controller().suggestType("AAPL").getBody();

        assertThat(body).containsEntry("requestType", "WATCHLIST_REVIEW");
    }

    @Test
    void unknownTickerYieldsDiscovery() {
        when(portfolioUseCase.listActivePositions()).thenReturn(List.of());
        when(watchlistUseCase.listAll()).thenReturn(List.of());

        var body = controller().suggestType("TSLA").getBody();

        assertThat(body).containsEntry("requestType", "DISCOVERY");
    }
}
