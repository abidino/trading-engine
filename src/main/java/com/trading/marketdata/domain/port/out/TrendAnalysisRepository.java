package com.trading.marketdata.domain.port.out;

import com.trading.marketdata.domain.model.TrendAnalysis;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Outbound port: persistence for daily technical-trend verdicts.
 */
public interface TrendAnalysisRepository {

    TrendAnalysis save(TrendAnalysis analysis);

    /** Most recent verdict for the ticker, if any. */
    Optional<TrendAnalysis> findLatestByTicker(String ticker);

    /** Up to {@code limit} most recent verdicts, newest first. */
    List<TrendAnalysis> findRecentByTicker(String ticker, int limit);

    /** Verdicts within an inclusive date range, newest first. */
    List<TrendAnalysis> findByTickerAndDateBetween(String ticker, LocalDate from, LocalDate to);

    /** Existing verdict for a ticker on a specific analysis date, if any. */
    Optional<TrendAnalysis> findByTickerAndDate(String ticker, LocalDate analysisDate);
}
