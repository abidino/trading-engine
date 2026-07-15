package com.trading.notification.web;

import com.trading.notification.domain.model.AlertLog;

public record AlertResponse(String ticker, String action, String channel, String sentAt, String status) {
    public static AlertResponse from(AlertLog a) {
        return new AlertResponse(a.ticker(), a.action(), a.channel().name(),
                a.sentAt() != null ? a.sentAt().toString() : null, a.deliveryStatus().name());
    }
}
