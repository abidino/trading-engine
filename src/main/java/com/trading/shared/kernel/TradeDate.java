package com.trading.shared.kernel;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Value Object: a trading date (market day).
 */
public record TradeDate(LocalDate value) {

    public TradeDate {
        Objects.requireNonNull(value, "Trade date must not be null");
    }

    public static TradeDate of(String isoDate) {
        return new TradeDate(LocalDate.parse(isoDate));
    }

    public static TradeDate today() {
        return new TradeDate(LocalDate.now());
    }

    public boolean isBefore(TradeDate other) {
        return this.value.isBefore(other.value);
    }

    public boolean isAfter(TradeDate other) {
        return this.value.isAfter(other.value);
    }

    @Override
    public String toString() {
        return value.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
