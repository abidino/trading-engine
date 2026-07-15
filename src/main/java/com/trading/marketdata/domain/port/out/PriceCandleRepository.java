package com.trading.marketdata.domain.port.out;

import com.trading.marketdata.domain.model.PriceCandle;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Outbound port: local candle persistence (acts as cache). */
public interface PriceCandleRepository {
    List<PriceCandle> findByTicker(String ticker);
    List<PriceCandle> findByTickerAndDateBetween(String ticker, LocalDate from, LocalDate to);
    Optional<PriceCandle> findByTickerAndDate(String ticker, LocalDate date);
    List<PriceCandle> findTickerByLastDay(Collection<String> ticker);
    PriceCandle save(PriceCandle candle);
    void saveAll(List<PriceCandle> candles);
}
