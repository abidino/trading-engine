package com.trading.marketdata.infrastructure;

import com.trading.marketdata.domain.model.PriceCandle;
import com.trading.marketdata.domain.model.TechnicalSignal;
import com.trading.marketdata.domain.port.out.MarketDataProviderPort;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic {@link MarketDataProviderPort} stub for the {@code mock} profile.
 * Generates a synthetic but realistic daily OHLCV series (business days only) so
 * technical-indicator and trend analysis run fully offline. The series has a mild
 * upward drift with oscillation, giving enough data points for EMA-200/RSI/MACD.
 */
@Primary
@Profile("mock")
@Component
public class MockMarketDataProvider implements MarketDataProviderPort {

    @Override
    public List<PriceCandle> fetchCandles(String ticker, LocalDate from, LocalDate to) {
        List<PriceCandle> candles = new ArrayList<>();
        // Per-ticker seed so different symbols produce visibly different series.
        double base = 50.0 + Math.floorMod(ticker.hashCode(), 250);
        double phase = Math.floorMod(ticker.hashCode(), 20) / 3.0;
        int i = 0;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            if (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY) {
                continue;
            }
            // Deterministic drift + oscillation.
            double drift = 0.25 * i;
            double wave = 6.0 * Math.sin((i + phase) / 9.0);
            double close = base + drift + wave;
            double open = close - Math.cos(i / 5.0);
            double high = Math.max(open, close) + 1.5;
            double low = Math.min(open, close) - 1.5;
            long volume = 1_000_000L + (i % 7) * 50_000L;
            candles.add(PriceCandle.create(
                    ticker, d,
                    bd(open), bd(high), bd(low), bd(close), volume));
            i++;
        }
        return candles;
    }

    @Override
    public List<TechnicalSignal> fetchIndicators(String ticker, LocalDate from, LocalDate to) {
        // Indicators are computed from candles by the domain calculator; provider
        // pre-computed signals are not needed in mock mode.
        return List.of();
    }

    private static BigDecimal bd(double v) {
        return BigDecimal.valueOf(v).setScale(4, RoundingMode.HALF_UP);
    }
}
