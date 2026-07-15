package com.trading.orchestration.web;

import com.trading.orchestration.domain.AnalysisRunStore.RunState;

public record AnalysisRunResponse(
        String runId, String ticker, String status,
        String startedAt, String completedAt,
        AnalysisResultResponse result, String error
) {
    public static AnalysisRunResponse from(RunState s) {
        return new AnalysisRunResponse(
                s.runId(), s.ticker(), s.status(),
                s.startedAt().toString(),
                s.completedAt() != null ? s.completedAt().toString() : null,
                s.result() != null ? AnalysisResultResponse.from(s.result()) : null,
                s.error()
        );
    }
}
