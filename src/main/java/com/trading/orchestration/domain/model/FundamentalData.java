package com.trading.orchestration.domain.model;

/**
 * Value Object: fundamental financial data for a ticker.
 */
public record FundamentalData(
        String ticker,
        Double peRatio,
        Double eps,
        Long marketCapUsd,
        Double revenueGrowthRate,
        Double debtToEquity,
        String sector,
        String industry,
        String businessDescription
) {
    public static FundamentalData empty(String ticker) {
        return new FundamentalData(ticker, null, null, null, null, null,
                "Unknown", "Unknown", "No fundamental data available.");
    }
}
