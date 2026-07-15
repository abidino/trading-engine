package com.trading.marketdata.domain.port.in;

import com.trading.marketdata.domain.model.IntradayQuote;

import java.util.List;
import java.util.Optional;

/**
 * Inbound port: intraday quote use cases for a single ticker.
 */
public interface IntradayQuoteUseCase {

    /** Fetch the latest quote from the provider and persist it. */
    IntradayQuote refreshQuote(String ticker);

    /** Most recent persisted quote for the ticker. */
    Optional<IntradayQuote> latestQuote(String ticker);

    /** Quotes captured within the last {@code sinceMinutes}, newest first. */
    List<IntradayQuote> quoteHistory(String ticker, int sinceMinutes);
}
