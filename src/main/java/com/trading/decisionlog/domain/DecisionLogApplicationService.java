package com.trading.decisionlog.domain;

import com.trading.decisionlog.domain.model.AccuracyReport;
import com.trading.decisionlog.domain.model.DecisionOutcome;
import com.trading.decisionlog.domain.model.DecisionRecord;
import com.trading.decisionlog.domain.port.out.DecisionRecordRepository;
import com.trading.decisionlog.domain.port.out.PriceLookupPort;
import com.trading.shared.kernel.Ticker;
import com.trading.shared.kernel.event.DecisionProduced;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Listens to DecisionProduced events and persists an immutable DecisionRecord.
 * Also provides query capabilities for the decision history view.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DecisionLogApplicationService {

    private final DecisionRecordRepository repository;
    private final PriceLookupPort priceLookup;

    @EventListener
    public void onDecisionProduced(DecisionProduced event) {
        DecisionRecord record = new DecisionRecord(
                UUID.randomUUID(),
                event.ticker(),
                event.requestType(),
                event.action(),
                event.confidence(),
                event.reasoning(),
                event.technicalSummary(),
                event.fundamentalSummary(),
                event.newsSummary(),
                event.socialSummary(),
                event.counterThesis(),
                event.keyRisks(),
                event.levels(),
                event.decidedAt()
        );
        repository.save(record);
        log.info("DecisionRecord saved: ticker={} action={}", event.ticker(), event.action());
    }

    @Transactional(readOnly = true)
    public List<DecisionRecord> listAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<DecisionRecord> listByTicker(Ticker ticker) {
        return repository.findByTicker(ticker);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<DecisionRecord> latestByTicker(Ticker ticker) {
        return repository.findLatestByTicker(ticker);
    }

    public void recordOutcome(UUID id, DecisionOutcome outcome) {
        repository.findById(id).ifPresent(r -> {
            r.recordOutcome(outcome);
            repository.save(r);
        });
    }

    // -----------------------------------------------------------------------
    // Automatic outcome evaluation (feedback loop)
    // -----------------------------------------------------------------------

    /**
     * Evaluates every still-PENDING decision older than {@code minAgeDays} against subsequent price
     * action and records a definitive VALIDATED / INVALIDATED outcome. Heuristic, band-tolerant:
     * <ul>
     *   <li>Bullish (BUY, ADD_TO_WATCHLIST): validated if price rose ≥ band%, else invalidated.</li>
     *   <li>Bearish (SELL, REMOVE): validated if price fell ≥ band%, else invalidated.</li>
     *   <li>Neutral (HOLD, WAIT, IGNORE): validated if price stayed within ±band%, else invalidated.</li>
     * </ul>
     * Decisions without enough price history to judge are left PENDING for a later run.
     *
     * @return the number of decisions newly resolved.
     */
    public int evaluatePendingOutcomes(int minAgeDays, double bandPct) {
        Instant cutoff = Instant.now().minus(minAgeDays, ChronoUnit.DAYS);
        int resolved = 0;
        for (DecisionRecord r : repository.findPending()) {
            if (r.getDecidedAt() == null || r.getDecidedAt().isAfter(cutoff)) {
                continue; // too recent to judge
            }
            String symbol = r.getTicker().value();
            var baseline = priceLookup.closeAsOf(symbol,
                    r.getDecidedAt().atZone(ZoneOffset.UTC).toLocalDate());
            var latest = priceLookup.latestClose(symbol);
            if (baseline.isEmpty() || latest.isEmpty() || baseline.get() == 0.0) {
                continue; // insufficient price data
            }
            double pct = (latest.get() - baseline.get()) / baseline.get() * 100.0;
            DecisionOutcome outcome = verdict(r.getAction(), pct, bandPct);
            r.recordOutcome(outcome);
            repository.save(r);
            resolved++;
            log.info("Outcome evaluated: ticker={} action={} move={}% -> {}",
                    symbol, r.getAction(), String.format("%.2f", pct), outcome);
        }
        log.info("Outcome evaluation complete — {} decisions resolved", resolved);
        return resolved;
    }

    /** Maps a decision's action + realised price move to a VALIDATED/INVALIDATED verdict. */
    static DecisionOutcome verdict(String action, double pct, double bandPct) {
        String a = action == null ? "" : action.toUpperCase();
        return switch (a) {
            case "BUY", "ADD_TO_WATCHLIST" ->
                    pct >= bandPct ? DecisionOutcome.VALIDATED : DecisionOutcome.INVALIDATED;
            case "SELL", "REMOVE" ->
                    pct <= -bandPct ? DecisionOutcome.VALIDATED : DecisionOutcome.INVALIDATED;
            default -> // HOLD, WAIT, IGNORE and any other neutral stance
                    Math.abs(pct) <= bandPct ? DecisionOutcome.VALIDATED : DecisionOutcome.INVALIDATED;
        };
    }

    /** Aggregated hit-rate across all evaluated decisions, broken down by action and request type. */
    @Transactional(readOnly = true)
    public AccuracyReport accuracy() {
        List<DecisionRecord> all = repository.findAll();
        long validated = all.stream().filter(r -> r.getOutcome() == DecisionOutcome.VALIDATED).count();
        long invalidated = all.stream().filter(r -> r.getOutcome() == DecisionOutcome.INVALIDATED).count();
        long pending = all.stream().filter(r -> r.getOutcome() == DecisionOutcome.PENDING).count();
        return AccuracyReport.of(validated, invalidated, pending,
                bucketize(all, DecisionRecord::getAction),
                bucketize(all, r -> r.getDecisionType().name()));
    }

    private List<AccuracyReport.Bucket> bucketize(
            List<DecisionRecord> records, java.util.function.Function<DecisionRecord, String> key) {
        Map<String, long[]> counts = new LinkedHashMap<>(); // key -> [validated, invalidated]
        for (DecisionRecord r : records) {
            if (r.getOutcome() == DecisionOutcome.PENDING) continue;
            long[] c = counts.computeIfAbsent(key.apply(r), k -> new long[2]);
            if (r.getOutcome() == DecisionOutcome.VALIDATED) c[0]++; else c[1]++;
        }
        return counts.entrySet().stream()
                .map(e -> AccuracyReport.Bucket.of(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();
    }
}
