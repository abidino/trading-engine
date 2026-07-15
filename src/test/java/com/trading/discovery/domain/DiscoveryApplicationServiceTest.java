package com.trading.discovery.domain;

import com.trading.discovery.domain.model.DiscoveryCandidate;
import com.trading.discovery.domain.model.DiscoveryFilter;
import com.trading.discovery.domain.model.DiscoveryStatus;
import com.trading.discovery.domain.model.PotentialStock;
import com.trading.discovery.domain.port.out.DiscoveryCandidateRepository;
import com.trading.discovery.domain.port.out.DismissedTickerRepository;
import com.trading.discovery.domain.port.out.SavedFilterRepository;
import com.trading.discovery.domain.port.out.StockScreenerPort;
import com.trading.marketdata.domain.model.TechnicalIndicatorSnapshot;
import com.trading.marketdata.domain.port.in.TechnicalAnalysisUseCase;
import com.trading.shared.kernel.Ticker;
import com.trading.shared.kernel.llm.LlmPort;
import com.trading.shared.kernel.llm.LlmResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the discovery cost/visibility rules:
 *  - only STRONG_UPTREND / UPTREND recommendations are surfaced,
 *  - non up-trend verdicts are parked (AUTO-suppressed) rather than re-analysed,
 *  - already-suppressed tickers are skipped without touching the LLM.
 */
class DiscoveryApplicationServiceTest {

    private StockScreenerPort screener;
    private DiscoveryCandidateRepository candidates;
    private SavedFilterRepository filters;
    private DismissedTickerRepository suppression;
    private TechnicalAnalysisUseCase technicals;
    private LlmPort llm;
    private ApplicationEventPublisher events;
    private DiscoveryApplicationService service;

    @BeforeEach
    void setUp() {
        screener = mock(StockScreenerPort.class);
        candidates = mock(DiscoveryCandidateRepository.class);
        filters = mock(SavedFilterRepository.class);
        suppression = mock(DismissedTickerRepository.class);
        technicals = mock(TechnicalAnalysisUseCase.class);
        llm = mock(LlmPort.class);
        events = mock(ApplicationEventPublisher.class);

        service = new DiscoveryApplicationService(
                screener, candidates, filters, suppression, technicals,
                llm, new ObjectMapper(), events);

        // Candidate persistence is an identity upsert for the test.
        when(candidates.findByTicker(anyString())).thenReturn(Optional.empty());
        when(candidates.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(technicals.computeIndicators(anyString(), anyInt())).thenReturn(snapshot());
    }

    @Test
    void onlyUptrendRecommendationsAreReturned_othersAreParked() {
        when(screener.screen(any())).thenReturn(List.of(
                stock("UP"), stock("FLAT"), stock("SUPP")));
        // SUPP is already suppressed -> must be skipped before any LLM call.
        when(suppression.isSuppressed("UP")).thenReturn(false);
        when(suppression.isSuppressed("FLAT")).thenReturn(false);
        when(suppression.isSuppressed("SUPP")).thenReturn(true);
        // Evaluated in screened order (SUPP skipped): UP then FLAT.
        when(llm.complete(any())).thenReturn(
                verdict(true, "STRONG_UPTREND"),
                verdict(false, "SIDEWAYS"));

        List<DiscoveryCandidate> results = service.runDiscoveryCycle(DiscoveryFilter.defaults());

        assertThat(results).extracting(DiscoveryCandidate::ticker).containsExactly("UP");
        verify(llm, times(2)).complete(any());              // SUPP never evaluated
        verify(suppression).suppress(eq("FLAT"), anyString()); // non up-trend parked
        verify(suppression, never()).suppress(eq("UP"), anyString());
        verify(suppression, never()).suppress(eq("SUPP"), anyString());
    }

    @Test
    void listRecommendationsHidesNonUptrendAndSuppressed() {
        when(candidates.findByStatus(DiscoveryStatus.RECOMMENDED)).thenReturn(List.of(
                recommended("GOOD", "UPTREND"),
                recommended("SIDE", "SIDEWAYS"),    // wrong trend -> hidden
                recommended("BLOCK", "UPTREND")));  // suppressed -> hidden
        when(suppression.isSuppressed("GOOD")).thenReturn(false);
        when(suppression.isSuppressed("BLOCK")).thenReturn(true);

        List<DiscoveryCandidate> shown = service.listRecommendations();

        assertThat(shown).extracting(DiscoveryCandidate::ticker).containsExactly("GOOD");
    }

    @Test
    void trendClassificationIsCaseAndSeparatorInsensitive() {
        assertThat(DiscoveryApplicationService.isUptrend("STRONG_UPTREND")).isTrue();
        assertThat(DiscoveryApplicationService.isUptrend("Uptrend")).isTrue();
        assertThat(DiscoveryApplicationService.isUptrend("strong-uptrend")).isTrue();
        assertThat(DiscoveryApplicationService.isUptrend("SIDEWAYS")).isFalse();
        assertThat(DiscoveryApplicationService.isUptrend("DOWNTREND")).isFalse();
        assertThat(DiscoveryApplicationService.isUptrend(null)).isFalse();
    }

    // -----------------------------------------------------------------------

    private static PotentialStock stock(String ticker) {
        return PotentialStock.create(new Ticker(ticker), "finviz", Map.of("sector", "Technology"));
    }

    private static DiscoveryCandidate recommended(String ticker, String trend) {
        return DiscoveryCandidate.screened(ticker, ticker, "Technology", "finviz", Map.of())
                .withEvaluation(true, 0.9, "ok", trend);
    }

    private static LlmResponse verdict(boolean recommend, String trend) {
        String json = "{\"recommend\":" + recommend + ",\"confidence\":0.8,"
                + "\"reasoning\":\"test\",\"trend\":\"" + trend + "\"}";
        return new LlmResponse(json, "test-model", 10, Duration.ofMillis(1));
    }

    private static TechnicalIndicatorSnapshot snapshot() {
        return new TechnicalIndicatorSnapshot(
                "X", LocalDate.now(), new BigDecimal("100"),
                1.0, 2.0, 3.0, 4.0, 5.0, 55.0, 0.1, 0.05, 0.05, 300);
    }
}
