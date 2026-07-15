package com.trading.orchestration.domain.port.out;

import java.util.List;

/**
 * Outbound port: read-only access to the marketdata module's support/resistance
 * computation, used both as prompt context and (elsewhere) for price-trigger checks.
 */
public interface SupportResistancePort {

    /**
     * @return the latest support/resistance snapshot for the ticker, or {@code null}
     *         when there is insufficient price history to compute it.
     */
    Snapshot fetchForTicker(String ticker);

    /** Compact, module-agnostic view of the computed levels. */
    record Snapshot(
            double close,
            Double nearestSupport,
            Double nearestResistance,
            List<Double> supports,
            List<Double> resistances
    ) {}
}
