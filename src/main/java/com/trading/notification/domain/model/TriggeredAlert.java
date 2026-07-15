package com.trading.notification.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A proactive threshold alert that has fired for a ticker on a given day.
 *
 * <p>The tuple {@code (ticker, alertType, triggeredOn)} is unique — it is the
 * de-duplication key that guarantees the same condition notifies the user at most
 * once per calendar day, preventing alert spam when the price hovers around a level.</p>
 */
public record TriggeredAlert(
        UUID id,
        String ticker,
        AlertType alertType,
        LocalDate triggeredOn,
        double price,
        double level,
        String message,
        Instant triggeredAt
) {
    public static TriggeredAlert of(String ticker, AlertType type, LocalDate day,
                                    double price, double level, String message) {
        return new TriggeredAlert(UUID.randomUUID(), ticker, type, day, price, level,
                message, Instant.now());
    }
}
