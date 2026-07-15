package com.trading.orchestration.domain.port.out;

import com.trading.orchestration.domain.model.TechnicalData;

/**
 * Outbound port: fetch OHLCV candles and computed technical indicators.
 * Implemented in infrastructure by the MarketDataTechnicalAdapter.
 */
public interface TechnicalDataPort {
    TechnicalData fetchForTicker(String ticker);

    /**
     * Ensures reasonably fresh candle/indicator data exists for the ticker before
     * it is read, syncing from the provider if the local cache is missing or stale.
     * Default: no-op.
     */
    default void ensureFresh(String ticker) {}
}
