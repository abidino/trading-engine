package com.trading.marketdata.domain.port.out;

import com.trading.marketdata.domain.model.PriceCandle;
import com.trading.marketdata.domain.model.TechnicalSignal;

import java.time.LocalDate;
import java.util.List;

/**
 * Outbound port: external market data provider (Yahoo Finance, Polygon.io, etc.).
 * Anticorruption Layer: provider-specific formats are translated here.
 */
public interface MarketDataProviderPort {
    List<PriceCandle> fetchCandles(String ticker, LocalDate from, LocalDate to);
    List<TechnicalSignal> fetchIndicators(String ticker, LocalDate from, LocalDate to);
}
