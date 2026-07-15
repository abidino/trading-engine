package com.trading.orchestration.domain.model;

import com.trading.shared.kernel.TradingLevels;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Value Object: output of the AI analysis pipeline.
 * Immutable — once produced, the result never changes.
 */
public record AnalysisResult(
        AnalysisAction action,
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
) {
    public AnalysisResult {
        Objects.requireNonNull(action, "Action must not be null");
        Objects.requireNonNull(reasoning, "Reasoning must not be null");
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence must be in [0.0, 1.0], got: " + confidence);
        }
        decidedAt = decidedAt != null ? decidedAt : Instant.now();
        technicalSummary = technicalSummary != null ? technicalSummary : "";
        fundamentalSummary = fundamentalSummary != null ? fundamentalSummary : "";
        newsSummary = newsSummary != null ? newsSummary : "";
        socialSummary = socialSummary != null ? socialSummary : "";
        counterThesis = counterThesis != null ? counterThesis : "";
        keyRisks = keyRisks != null ? List.copyOf(keyRisks) : List.of();
        levels = levels != null ? levels : TradingLevels.empty();
    }
}
