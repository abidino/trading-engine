package com.trading.notification.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity: a record of one alert delivery attempt.
 */
public record AlertLog(
        UUID id,
        String ticker,
        String action,
        AlertChannel channel,
        Instant sentAt,
        DeliveryStatus deliveryStatus,
        String errorMessage
) {
    public static AlertLog sent(String ticker, String action, AlertChannel channel) {
        return new AlertLog(UUID.randomUUID(), ticker, action, channel, Instant.now(), DeliveryStatus.SENT, null);
    }

    public static AlertLog failed(String ticker, String action, AlertChannel channel, String error) {
        return new AlertLog(UUID.randomUUID(), ticker, action, channel, Instant.now(), DeliveryStatus.FAILED, error);
    }

    public static AlertLog skipped(String ticker, String action, AlertChannel channel) {
        return new AlertLog(UUID.randomUUID(), ticker, action, channel, Instant.now(), DeliveryStatus.SKIPPED, null);
    }
}
