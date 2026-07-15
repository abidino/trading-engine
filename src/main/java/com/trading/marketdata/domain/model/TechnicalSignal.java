package com.trading.marketdata.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Value Object: a computed technical indicator value for a ticker on a given date.
 */
public record TechnicalSignal(
        UUID id,
        String ticker,
        String indicatorName,
        BigDecimal value,
        LocalDate signalDate
) {
    public static TechnicalSignal create(String ticker, String indicator,
                                         BigDecimal value, LocalDate date) {
        return new TechnicalSignal(UUID.randomUUID(), ticker, indicator, value, date);
    }
}
