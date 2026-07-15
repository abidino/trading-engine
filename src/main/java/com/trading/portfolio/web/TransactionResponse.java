package com.trading.portfolio.web;

import com.trading.portfolio.domain.model.PortfolioTransaction;

import java.util.UUID;

public record TransactionResponse(
        UUID id, String ticker, String transactionType,
        double quantity, double price, double commission, String executedAt
) {
    public static TransactionResponse from(PortfolioTransaction t) {
        return new TransactionResponse(t.id(), t.ticker().value(), t.transactionType().name(),
                t.quantity().doubleValue(), t.price().amount().doubleValue(),
                t.commission().amount().doubleValue(), t.executedAt().toString());
    }
}
