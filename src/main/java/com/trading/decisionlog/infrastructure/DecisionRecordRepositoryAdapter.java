package com.trading.decisionlog.infrastructure;

import com.trading.decisionlog.domain.model.DecisionOutcome;
import com.trading.decisionlog.domain.model.DecisionRecord;
import com.trading.decisionlog.domain.port.out.DecisionRecordRepository;
import com.trading.shared.kernel.AnalysisRequestType;
import com.trading.shared.kernel.Ticker;
import com.trading.shared.kernel.TradingLevels;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DecisionRecordRepositoryAdapter implements DecisionRecordRepository {

    private final JpaDecisionRecordRepository jpa;

    @Override
    public DecisionRecord save(DecisionRecord record) {
        return toDomain(jpa.save(toEntity(record)));
    }

    @Override
    public Optional<DecisionRecord> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<DecisionRecord> findAll() {
        return jpa.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<DecisionRecord> findByTicker(Ticker ticker) {
        return jpa.findByTickerOrderByDecidedAtDesc(ticker.value()).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<DecisionRecord> findLatestByTicker(Ticker ticker) {
        return jpa.findFirstByTickerOrderByDecidedAtDesc(ticker.value()).map(this::toDomain);
    }

    @Override
    public List<DecisionRecord> findPending() {
        return jpa.findByOutcome("PENDING").stream().map(this::toDomain).toList();
    }

    private DecisionRecord toDomain(DecisionRecordEntity e) {
        DecisionRecord r = new DecisionRecord(
                e.getId(), new Ticker(e.getTicker()),
                AnalysisRequestType.valueOf(e.getDecisionType()),
                e.getAction(), e.getConfidence(), e.getReasoning(),
                e.getTechnicalSummary(), e.getFundamentalSummary(),
                e.getNewsSummary(), e.getSocialSummary(),
                e.getCounterThesis(), splitRisks(e.getKeyRisks()),
                new TradingLevels(e.getEntryLow(), e.getEntryHigh(),
                        e.getAggressiveEntry(), e.getIdealEntry(), e.getSafeEntry(),
                        e.getStopLoss(), e.getTakeProfit(),
                        e.getNearestSupport(), e.getNearestResistance()),
                e.getDecidedAt());
        if (!"PENDING".equals(e.getOutcome())) {
            r.recordOutcome(DecisionOutcome.valueOf(e.getOutcome()));
        }
        return r;
    }

    private DecisionRecordEntity toEntity(DecisionRecord r) {
        TradingLevels l = r.getLevels() != null ? r.getLevels() : TradingLevels.empty();
        return DecisionRecordEntity.builder()
                .id(r.getId()).ticker(r.getTicker().value())
                .decisionType(r.getDecisionType().name()).action(r.getAction())
                .confidence(r.getConfidence()).reasoning(r.getReasoning())
                .technicalSummary(r.getTechnicalSummary()).fundamentalSummary(r.getFundamentalSummary())
                .newsSummary(r.getNewsSummary()).socialSummary(r.getSocialSummary())
                .counterThesis(r.getCounterThesis()).keyRisks(joinRisks(r.getKeyRisks()))
                .entryLow(l.entryLow()).entryHigh(l.entryHigh())
                .aggressiveEntry(l.aggressiveEntry()).idealEntry(l.idealEntry()).safeEntry(l.safeEntry())
                .stopLoss(l.stopLoss()).takeProfit(l.takeProfit())
                .nearestSupport(l.nearestSupport()).nearestResistance(l.nearestResistance())
                .decidedAt(r.getDecidedAt()).outcome(r.getOutcome().name())
                .evaluatedAt(r.getEvaluatedAt())
                .build();
    }

    /** keyRisks persist as newline-joined text (one risk per line). */
    private static String joinRisks(List<String> risks) {
        return (risks == null || risks.isEmpty()) ? null : String.join("\n", risks);
    }

    private static List<String> splitRisks(String joined) {
        if (joined == null || joined.isBlank()) return List.of();
        return java.util.Arrays.stream(joined.split("\n"))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
