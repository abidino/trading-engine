package com.trading.web;

import java.util.List;
import java.util.Map;

public record DashboardSummary(
        double totalMarketValue,
        double totalCostBasis,
        double totalUnrealizedPnl,
        double totalUnrealizedPnlPercent,
        int positionCount,
        Map<String, Double> sectorAllocation,
        List<TickerPnl> topGainers,
        List<TickerPnl> topLosers,
        int watchlistCount,
        double decisionAccuracy
) {}
