package com.trading.notification.web;

import com.trading.notification.domain.model.TriggeredAlert;

/** API view of a proactive threshold alert. */
public record TriggeredAlertResponse(
        String ticker,
        String alertType,
        String triggeredOn,
        double price,
        double level,
        String message,
        String triggeredAt
) {
    public static TriggeredAlertResponse from(TriggeredAlert a) {
        return new TriggeredAlertResponse(
                a.ticker(), a.alertType().name(),
                a.triggeredOn() != null ? a.triggeredOn().toString() : null,
                a.price(), a.level(), a.message(),
                a.triggeredAt() != null ? a.triggeredAt().toString() : null);
    }
}
