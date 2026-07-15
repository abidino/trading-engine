package com.trading.portfolio.web;

public record PortfolioSummaryResponse(
        double totalMarketValue, double totalGains, double totalLosses, double totalPnl,
        double netPnl, double totalCommissions, int positionCount, int gainPositionCount, int lossPositionCount
) {}
