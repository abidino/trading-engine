package com.trading.portfolio.web;

import com.trading.portfolio.domain.model.PortfolioPosition;

import java.math.BigDecimal;
import java.util.UUID;
public record PositionResponse(
        UUID id,
        String ticker,
        BigDecimal quantity,
        BigDecimal averageCost,
        BigDecimal currentPrice,
        BigDecimal marketValue,
        BigDecimal unrealizedPnl,
        BigDecimal unrealizedPnlPercent,
        String sector,
        String assetType,
        boolean active
) {
    public static PositionResponse from(PortfolioPosition p) {
        BigDecimal qty        = p.getQuantity();
        BigDecimal avgCost    = p.getAverageEntryPrice().amount();
        BigDecimal currPrice  = p.effectiveCurrentPrice().amount();
        BigDecimal mktValue   = p.marketValue();
        BigDecimal pnl        = p.unrealizedPnl();
        BigDecimal pnlPct     = p.unrealizedPnlPercent();

        return new PositionResponse(
                p.getId(),
                p.getTicker().value(),
                qty,
                avgCost,
                currPrice,
                mktValue,
                pnl,
                pnlPct,
                null,
                "STOCK",
                p.isActive()
        );
    }
}
