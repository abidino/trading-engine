package com.trading.decisionlog.domain.port.out;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Outbound port: lets the decision-log evaluate outcomes against actual price action
 * without depending on the market-data module directly. Implemented in infrastructure.
 */
public interface PriceLookupPort {
    /** Closing price on the given date, or the most recent close on/before it. Empty if none. */
    Optional<Double> closeAsOf(String ticker, LocalDate date);

    /** Most recent available closing price for the ticker. Empty if no candles exist. */
    Optional<Double> latestClose(String ticker);
}
