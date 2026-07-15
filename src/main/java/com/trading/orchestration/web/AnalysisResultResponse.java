package com.trading.orchestration.web;

import com.trading.orchestration.domain.model.AnalysisResult;
import com.trading.shared.kernel.TradingLevels;

import java.util.List;
import java.util.Map;

public record AnalysisResultResponse(
        String action, double confidence, String reasoning,
        String technicalSummary, String fundamentalSummary,
        String newsSummary, String socialSummary,
        String counterThesis, List<String> keyRisks,
        String decidedAt,
        TradingLevels levels,
        Map<String, String> analystReports
) {
    public static AnalysisResultResponse from(AnalysisResult r) {
        return new AnalysisResultResponse(
                r.action().name(), r.confidence(), r.reasoning(),
                r.technicalSummary(), r.fundamentalSummary(),
                r.newsSummary(), r.socialSummary(),
                r.counterThesis(), r.keyRisks(),
                r.decidedAt().toString(),
                r.levels(),
                Map.of()  // analystReports: filled in future iteration
        );
    }
}
