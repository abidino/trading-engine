package com.trading.marketdata.web;

import com.trading.marketdata.domain.model.TrendAnalysis;

/** API view of a persisted daily trend verdict, including the indicator snapshot. */
public record TrendAnalysisResponse(
        String ticker,
        String analysisDate,
        String trend,
        double confidence,
        String reasoning,
        TechnicalIndicatorResponse indicators,
        String llmModel,
        String createdAt
) {
    public static TrendAnalysisResponse from(TrendAnalysis t) {
        return new TrendAnalysisResponse(
                t.ticker(),
                t.analysisDate().toString(),
                t.trend().name(),
                t.confidence(),
                t.reasoning(),
                TechnicalIndicatorResponse.from(t.snapshot()),
                t.llmModel(),
                t.createdAt().toString());
    }
}
