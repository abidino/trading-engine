package com.trading.shared.kernel.event;

import com.trading.shared.kernel.AnalysisRequestType;
import com.trading.shared.kernel.DomainEvent;
import com.trading.shared.kernel.Ticker;
import com.trading.shared.kernel.TradingLevels;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Cross-domain event: AI analysis pipeline has produced a decision.
 *
 * Published by: ai-orchestration.
 * Consumed by: decision-log (writes audit record), notification (sends alert).
 */
public record DecisionProduced(
        Ticker ticker,
        AnalysisRequestType requestType,
        String action,
        double confidence,
        String reasoning,
        String technicalSummary,
        String fundamentalSummary,
        String newsSummary,
        String socialSummary,
        String counterThesis,
        List<String> keyRisks,
        TradingLevels levels,
        Instant decidedAt
) implements DomainEvent {

    public DecisionProduced {
        Objects.requireNonNull(ticker, "Ticker must not be null");
        Objects.requireNonNull(action, "Action must not be null");
        Objects.requireNonNull(reasoning, "Reasoning must not be null");
        decidedAt = decidedAt != null ? decidedAt : Instant.now();
        counterThesis = counterThesis != null ? counterThesis : "";
        keyRisks = keyRisks != null ? List.copyOf(keyRisks) : List.of();
        levels = levels != null ? levels : TradingLevels.empty();
    }
}
