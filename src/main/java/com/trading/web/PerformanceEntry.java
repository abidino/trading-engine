package com.trading.web;

public record PerformanceEntry(
        String ticker,
        double marketValue,
        double costBasis,
        double unrealizedPnl,
        double unrealizedPnlPercent,
        int decisionCount,
        double winRate
) {}
