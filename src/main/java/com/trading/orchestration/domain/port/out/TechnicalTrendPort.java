package com.trading.orchestration.domain.port.out;

/**
 * Outbound port: read-only access to the marketdata module's daily technical-trend
 * history, summarised for inclusion in an analysis prompt.
 */
public interface TechnicalTrendPort {

    /**
     * @return a concise, human-readable summary of the ticker's recent daily trend
     *         verdicts (latest verdict + distribution over the recent window), or a
     *         short "no data" message when none exist.
     */
    String fetchTrendSummary(String ticker);
}
