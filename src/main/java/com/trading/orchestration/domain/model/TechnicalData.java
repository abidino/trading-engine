package com.trading.orchestration.domain.model;

import java.util.Map;

/**
 * Value Object: technical market data for a ticker.
 * Carries the raw indicator map (RSI, MACD, SMA200…) and recent price history as a text summary.
 */
public record TechnicalData(
        String ticker,
        Map<String, Double> indicators,
        String priceHistorySummary
) {
    public TechnicalData {
        indicators = indicators != null ? Map.copyOf(indicators) : Map.of();
        priceHistorySummary = priceHistorySummary != null ? priceHistorySummary : "";
    }

    public static TechnicalData empty(String ticker) {
        return new TechnicalData(ticker, Map.of(), "No technical data available.");
    }

    public Double indicator(String name) {
        return indicators.get(name);
    }
}
