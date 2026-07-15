package com.trading.decisionlog.domain;

import com.trading.decisionlog.domain.model.AccuracyReport;
import com.trading.decisionlog.domain.model.DecisionOutcome;
import com.trading.decisionlog.domain.model.DecisionRecord;
import com.trading.decisionlog.domain.port.out.DecisionRecordRepository;
import com.trading.decisionlog.domain.port.out.PriceLookupPort;
import com.trading.shared.kernel.AnalysisRequestType;
import com.trading.shared.kernel.Ticker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the decision-journal feedback loop: PENDING decisions self-resolve against realised price
 * action, and the aggregate hit-rate is computed correctly.
 */
@ExtendWith(MockitoExtension.class)
class DecisionOutcomeEvaluationTest {

    @Mock DecisionRecordRepository repository;
    @Mock PriceLookupPort priceLookup;

    private DecisionLogApplicationService service() {
        return new DecisionLogApplicationService(repository, priceLookup);
    }

    private DecisionRecord decision(String action, Instant decidedAt) {
        return new DecisionRecord(UUID.randomUUID(), new Ticker("NVDA"),
                AnalysisRequestType.WATCHLIST_REVIEW, action, 0.8, "reason",
                "tech", "fund", "news", "social", decidedAt);
    }

    // ── verdict rules ─────────────────────────────────────────────────────────

    @Test
    void verdictRules() {
        assertThat(DecisionLogApplicationService.verdict("BUY", 10.0, 3.0)).isEqualTo(DecisionOutcome.VALIDATED);
        assertThat(DecisionLogApplicationService.verdict("BUY", 1.0, 3.0)).isEqualTo(DecisionOutcome.INVALIDATED);
        assertThat(DecisionLogApplicationService.verdict("SELL", -10.0, 3.0)).isEqualTo(DecisionOutcome.VALIDATED);
        assertThat(DecisionLogApplicationService.verdict("SELL", 10.0, 3.0)).isEqualTo(DecisionOutcome.INVALIDATED);
        assertThat(DecisionLogApplicationService.verdict("HOLD", 1.0, 3.0)).isEqualTo(DecisionOutcome.VALIDATED);
        assertThat(DecisionLogApplicationService.verdict("HOLD", 8.0, 3.0)).isEqualTo(DecisionOutcome.INVALIDATED);
    }

    // ── evaluation job ────────────────────────────────────────────────────────

    @Test
    void resolvesOldBuyThatRoseAsValidated() {
        DecisionRecord old = decision("BUY", Instant.now().minus(10, ChronoUnit.DAYS));
        when(repository.findPending()).thenReturn(List.of(old));
        when(priceLookup.closeAsOf(any(), any())).thenReturn(Optional.of(100.0));
        when(priceLookup.latestClose(any())).thenReturn(Optional.of(115.0)); // +15%

        int resolved = service().evaluatePendingOutcomes(5, 3.0);

        assertThat(resolved).isEqualTo(1);
        ArgumentCaptor<DecisionRecord> saved = ArgumentCaptor.forClass(DecisionRecord.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getOutcome()).isEqualTo(DecisionOutcome.VALIDATED);
    }

    @Test
    void resolvesOldBuyThatFellAsInvalidated() {
        DecisionRecord old = decision("BUY", Instant.now().minus(10, ChronoUnit.DAYS));
        when(repository.findPending()).thenReturn(List.of(old));
        when(priceLookup.closeAsOf(any(), any())).thenReturn(Optional.of(100.0));
        when(priceLookup.latestClose(any())).thenReturn(Optional.of(90.0)); // -10%

        service().evaluatePendingOutcomes(5, 3.0);

        ArgumentCaptor<DecisionRecord> saved = ArgumentCaptor.forClass(DecisionRecord.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getOutcome()).isEqualTo(DecisionOutcome.INVALIDATED);
    }

    @Test
    void tooRecentDecisionIsLeftPending() {
        DecisionRecord recent = decision("BUY", Instant.now().minus(1, ChronoUnit.DAYS));
        when(repository.findPending()).thenReturn(List.of(recent));
        lenient().when(priceLookup.closeAsOf(any(), any())).thenReturn(Optional.of(100.0));
        lenient().when(priceLookup.latestClose(any())).thenReturn(Optional.of(115.0));

        int resolved = service().evaluatePendingOutcomes(5, 3.0);

        assertThat(resolved).isZero();
        verify(repository, never()).save(any());
    }

    @Test
    void insufficientPriceDataIsSkipped() {
        DecisionRecord old = decision("BUY", Instant.now().minus(10, ChronoUnit.DAYS));
        when(repository.findPending()).thenReturn(List.of(old));
        when(priceLookup.closeAsOf(any(), any())).thenReturn(Optional.empty());

        int resolved = service().evaluatePendingOutcomes(5, 3.0);

        assertThat(resolved).isZero();
        verify(repository, never()).save(any());
    }

    // ── accuracy aggregation ──────────────────────────────────────────────────

    @Test
    void accuracyComputesHitRateExcludingPending() {
        DecisionRecord win = decision("BUY", Instant.now());
        win.recordOutcome(DecisionOutcome.VALIDATED);
        DecisionRecord loss = decision("SELL", Instant.now());
        loss.recordOutcome(DecisionOutcome.INVALIDATED);
        DecisionRecord win2 = decision("BUY", Instant.now());
        win2.recordOutcome(DecisionOutcome.VALIDATED);
        DecisionRecord pending = decision("HOLD", Instant.now());
        when(repository.findAll()).thenReturn(List.of(win, loss, win2, pending));

        AccuracyReport report = service().accuracy();

        assertThat(report.total()).isEqualTo(4);
        assertThat(report.validated()).isEqualTo(2);
        assertThat(report.invalidated()).isEqualTo(1);
        assertThat(report.pending()).isEqualTo(1);
        assertThat(report.hitRate()).isEqualTo(2.0 / 3.0);
        assertThat(report.byAction()).anySatisfy(b -> {
            if (b.key().equals("BUY")) assertThat(b.validated()).isEqualTo(2);
        });
    }
}
