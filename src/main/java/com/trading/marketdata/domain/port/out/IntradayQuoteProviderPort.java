package com.trading.marketdata.domain.port.out;

import com.trading.marketdata.domain.model.IntradayQuote;

import java.util.Optional;

/** Outbound port: live intraday quote provider (Yahoo, etc.). */
public interface IntradayQuoteProviderPort {

    /** Current quote for the ticker, or empty when unavailable. */
    Optional<IntradayQuote> fetchQuote(String ticker);
}
