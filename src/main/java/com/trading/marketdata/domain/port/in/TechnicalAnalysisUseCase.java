package com.trading.marketdata.domain.port.in;

import com.trading.marketdata.domain.model.SupportResistanceLevels;
import com.trading.marketdata.domain.model.TechnicalIndicatorSnapshot;
import com.trading.marketdata.domain.model.TrendAnalysis;

import java.util.List;
import java.util.Optional;

/**
 * Inbound port: technical-analysis use cases for a single ticker.
 *
 * Two distinct capabilities:
 * <ul>
 *   <li>{@link #computeIndicators} — pure math, no LLM, no persistence (quick inspection).</li>
 *   <li>{@link #analyzeTrend} — compute indicators, ask the LLM for a trend verdict, persist it.</li>
 * </ul>
 */
public interface TechnicalAnalysisUseCase {

    /** Sync recent candles and compute the indicator snapshot. Does NOT call the LLM or persist. */
    TechnicalIndicatorSnapshot computeIndicators(String ticker, int lookbackDays);

    /** Compute support &amp; resistance levels from recent candles. No LLM, no persistence. */
    SupportResistanceLevels supportResistance(String ticker, int lookbackDays);

    /** Compute indicators, obtain an LLM trend verdict, persist and return it. */
    TrendAnalysis analyzeTrend(String ticker, int lookbackDays);

    /** Latest persisted trend verdict for the ticker. */
    Optional<TrendAnalysis> latestTrend(String ticker);

    /** Recent persisted trend verdicts, newest first. */
    List<TrendAnalysis> trendHistory(String ticker, int limit);
}
