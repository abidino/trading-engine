package com.trading.shared.kernel;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object: a stock/ETF/crypto ticker symbol.
 * Normalises to uppercase; accepts letters, digits and dots (e.g. BRK.A).
 */
public record Ticker(String value) {

    private static final Pattern VALID = Pattern.compile("^[A-Z0-9.]{1,10}$");

    public Ticker {
        Objects.requireNonNull(value, "Ticker must not be null");
        value = value.trim().toUpperCase();
        if (!VALID.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid ticker symbol: '" + value + "'");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
