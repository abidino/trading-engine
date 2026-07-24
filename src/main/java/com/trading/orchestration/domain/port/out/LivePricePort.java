package com.trading.orchestration.domain.port.out;

/**
 * Outbound port: the most recent <b>live</b> intraday price for a ticker, spanning
 * pre-market / regular / post-market sessions.
 *
 * <p>Unlike {@link SupportResistancePort} and {@link TechnicalDataPort} — which are anchored to
 * end-of-day OHLC candles — this port exposes the freshest quote Yahoo can give at the moment of
 * analysis, so entry/stop levels are evaluated against the true latest price regardless of the
 * trading session (including overnight when the last print is the pre/post-market price).</p>
 */
public interface LivePricePort {

    /**
     * @return the freshest live quote snapshot for the ticker, or {@code null} when no live
     *         quote is available (e.g. provider failure or unknown ticker).
     */
    Snapshot fetchForTicker(String ticker);

    /**
     * Refreshes the live quote from the provider so a subsequent {@link #fetchForTicker(String)}
     * reads the most up-to-date price. Failures are swallowed by the implementation. Default: no-op.
     */
    default void ensureFresh(String ticker) {}

    /** Compact, module-agnostic view of a live quote. */
    record Snapshot(
            double price,
            String session,
            Double previousClose,
            Double changePercent,
            String quoteTime
    ) {}
}
