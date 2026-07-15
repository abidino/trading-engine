package com.trading.decisionlog.infrastructure;

import com.trading.decisionlog.domain.port.out.PriceLookupPort;
import com.trading.marketdata.domain.model.PriceCandle;
import com.trading.marketdata.domain.port.out.PriceCandleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Bridges the market-data module's persisted candles into the decision-log's
 * {@link PriceLookupPort}, so outcome evaluation can compare a decision against
 * subsequent price action.
 */
@Component
@RequiredArgsConstructor
public class PriceLookupAdapter implements PriceLookupPort {

    private final PriceCandleRepository candleRepository;

    @Override
    public Optional<Double> closeAsOf(String ticker, LocalDate date) {
        // Exact-day candle if present, else the latest candle on/before the date.
        Optional<PriceCandle> exact = candleRepository.findByTickerAndDate(ticker, date);
        if (exact.isPresent()) {
            return exact.map(c -> c.close().doubleValue());
        }
        return candleRepository.findByTicker(ticker).stream()
                .filter(c -> !c.candleDate().isAfter(date))
                .max((a, b) -> a.candleDate().compareTo(b.candleDate()))
                .map(c -> c.close().doubleValue());
    }

    @Override
    public Optional<Double> latestClose(String ticker) {
        return candleRepository.findByTicker(ticker).stream()
                .max((a, b) -> a.candleDate().compareTo(b.candleDate()))
                .map(c -> c.close().doubleValue());
    }
}
