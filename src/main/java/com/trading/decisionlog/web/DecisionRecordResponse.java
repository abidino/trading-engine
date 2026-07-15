package com.trading.decisionlog.web;

import com.trading.decisionlog.domain.model.DecisionRecord;
import com.trading.shared.kernel.TradingLevels;

import java.util.List;
import java.util.UUID;

public record DecisionRecordResponse(
        UUID id, String ticker, String decisionType, String action,
        double confidence, String reasoning, String newsSummary, String socialSummary,
        String technicalSummary, String fundamentalSummary,
        String counterThesis, List<String> keyRisks,
        TradingLevels levels,
        String decidedAt, String outcome, String evaluatedAt
) {
    public static DecisionRecordResponse from(DecisionRecord r) {
        return new DecisionRecordResponse(
                r.getId(), r.getTicker().value(), r.getDecisionType().name(),
                r.getAction(), r.getConfidence(), r.getReasoning(),
                r.getNewsSummary(), r.getSocialSummary(),
                r.getTechnicalSummary(), r.getFundamentalSummary(),
                r.getCounterThesis(), r.getKeyRisks(),
                r.getLevels(),
                r.getDecidedAt().toString(), r.getOutcome().name(),
                r.getEvaluatedAt() != null ? r.getEvaluatedAt().toString() : null);
    }
}
