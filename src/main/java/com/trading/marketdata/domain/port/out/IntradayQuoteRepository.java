package com.trading.marketdata.domain.port.out;

import com.trading.marketdata.domain.model.IntradayQuote;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Outbound port: persistence for intraday quotes. */
public interface IntradayQuoteRepository {

    IntradayQuote save(IntradayQuote quote);

    Optional<IntradayQuote> findLatestByTicker(String ticker);

    /** Quotes for a ticker captured on/after {@code since}, newest first. */
    List<IntradayQuote> findByTickerSince(String ticker, Instant since);
}
