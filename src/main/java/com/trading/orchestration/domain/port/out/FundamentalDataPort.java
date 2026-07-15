package com.trading.orchestration.domain.port.out;

import com.trading.orchestration.domain.model.FundamentalData;

/**
 * Outbound port: fetch fundamental financial data (P/E, EPS, market cap…).
 * Implemented in infrastructure by the YahooFundamentalsAdapter.
 */
public interface FundamentalDataPort {
    FundamentalData fetchForTicker(String ticker);
}
