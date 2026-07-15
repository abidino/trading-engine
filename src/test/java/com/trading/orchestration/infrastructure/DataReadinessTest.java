package com.trading.orchestration.infrastructure;

import com.trading.intelligence.domain.IntelligenceApplicationService;
import com.trading.intelligence.domain.model.NewsArticle;
import com.trading.intelligence.domain.model.NewsCategory;
import com.trading.intelligence.domain.model.SocialSignal;
import com.trading.intelligence.domain.port.out.NewsRepository;
import com.trading.intelligence.domain.port.out.SocialSignalRepository;
import com.trading.marketdata.domain.model.PriceCandle;
import com.trading.marketdata.domain.port.in.TechnicalAnalysisUseCase;
import com.trading.marketdata.domain.port.out.PriceCandleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the on-demand data-gathering ("readiness") behaviour of the orchestration
 * adapters: stale/missing local data triggers a provider fetch, fresh data is reused
 * without an extra network call.
 */
@ExtendWith(MockitoExtension.class)
class DataReadinessTest {

    @Mock NewsRepository newsRepository;
    @Mock SocialSignalRepository socialSignalRepository;
    @Mock PriceCandleRepository priceCandleRepository;
    @Mock IntelligenceApplicationService intelligenceService;
    @Mock TechnicalAnalysisUseCase technicalAnalysisUseCase;

    // ── News ────────────────────────────────────────────────────────────────

    @Test
    void newsStaleTriggersScan() {
        when(newsRepository.findForTickerSince(any(), any())).thenReturn(List.of());
        var adapter = new NewsIntelligenceAdapter(newsRepository, intelligenceService, 7, 30, 15);

        adapter.ensureFresh("NVDA");

        verify(intelligenceService).scanTickerNews("NVDA", 15);
    }

    @Test
    void newsFreshSkipsScan() {
        NewsArticle recent = NewsArticle.create(
                "http://x", "headline", "src", NewsCategory.STOCK,
                "summary", Instant.now(), List.of()); // capturedAt = now
        when(newsRepository.findForTickerSince(any(), any())).thenReturn(List.of(recent));
        var adapter = new NewsIntelligenceAdapter(newsRepository, intelligenceService, 7, 30, 15);

        adapter.ensureFresh("NVDA");

        verify(intelligenceService, never()).scanTickerNews(any(), anyInt());
    }

    // ── Social ──────────────────────────────────────────────────────────────

    @Test
    void socialStaleTriggersCollect() {
        when(socialSignalRepository.findByTicker(any())).thenReturn(List.of());
        var adapter = new SocialIntelligenceAdapter(socialSignalRepository, intelligenceService, 30, 20);

        adapter.ensureFresh("NVDA");

        verify(intelligenceService).collectSocialSignalsForTicker("NVDA", 20);
    }

    @Test
    void socialFreshSkipsCollect() {
        SocialSignal recent = SocialSignal.create("NVDA", "reddit", "content", 1.0, 0.5); // now
        when(socialSignalRepository.findByTicker(any())).thenReturn(List.of(recent));
        var adapter = new SocialIntelligenceAdapter(socialSignalRepository, intelligenceService, 30, 20);

        adapter.ensureFresh("NVDA");

        verify(intelligenceService, never()).collectSocialSignalsForTicker(any(), anyInt());
    }

    // ── Technical ─────────────────────────────────────────────────────────────

    @Test
    void technicalStaleTriggersSync() {
        LocalDate old = LocalDate.now().minusDays(10);
        when(priceCandleRepository.findByTicker(any())).thenReturn(List.of(candle(old)));
        var adapter = new TechnicalDataAdapter(priceCandleRepository, technicalAnalysisUseCase, 90);

        adapter.ensureFresh("NVDA");

        verify(technicalAnalysisUseCase).computeIndicators("NVDA", 90);
    }

    @Test
    void technicalFreshSkipsSync() {
        PriceCandle fresh = candle(lastTradingDay(LocalDate.now()));
        when(priceCandleRepository.findByTicker(any())).thenReturn(List.of(fresh));
        var adapter = new TechnicalDataAdapter(priceCandleRepository, technicalAnalysisUseCase, 90);

        adapter.ensureFresh("NVDA");

        verify(technicalAnalysisUseCase, never()).computeIndicators(any(), anyInt());
    }

    private PriceCandle candle(LocalDate date) {
        return PriceCandle.create("NVDA", date,
                BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, 100L);
    }

    private LocalDate lastTradingDay(LocalDate today) {
        if (today.getDayOfWeek() == DayOfWeek.SATURDAY) return today.minusDays(1);
        if (today.getDayOfWeek() == DayOfWeek.SUNDAY) return today.minusDays(2);
        return today;
    }
}
