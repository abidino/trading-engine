package com.trading.portfolio.web;

public record SoldPositionResponse(
        String ticker, double totalQuantity, double avgBuyPrice, double avgSellPrice,
        double totalBuyCommission, double totalSellCommission, double totalCommission,
        double realizedPnl, String soldAt
) {}
