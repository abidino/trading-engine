package com.trading.orchestration.infrastructure;

import com.trading.marketdata.domain.port.in.TechnicalAnalysisUseCase;
import com.trading.marketdata.domain.port.out.PriceCandleRepository;
import com.trading.orchestration.domain.model.TechnicalData;
import com.trading.orchestration.domain.port.out.TechnicalDataPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Bridges the market-data module's persisted candles into the format
 * the ai-orchestration domain expects (TechnicalData with indicators map).
 * Syncs fresh candles on demand when the local cache is missing or stale.
 */
@Slf4j
@Component
public class TechnicalDataAdapter implements TechnicalDataPort {

    private final PriceCandleRepository priceCandleRepository;
    private final TechnicalAnalysisUseCase technicalAnalysisUseCase;
    private final int lookbackDays;

    public TechnicalDataAdapter(
            PriceCandleRepository priceCandleRepository,
            TechnicalAnalysisUseCase technicalAnalysisUseCase,
            @Value("${orchestration.freshness.technical-lookback-days:90}") int lookbackDays) {
        this.priceCandleRepository = priceCandleRepository;
        this.technicalAnalysisUseCase = technicalAnalysisUseCase;
        this.lookbackDays = lookbackDays;
    }

    @Override
    public void ensureFresh(String ticker) {
        LocalDate lastTradingDay = lastTradingDay(LocalDate.now());
        LocalDate latest = priceCandleRepository.findByTicker(ticker).stream()
                .map(c -> c.candleDate())
                .max(LocalDate::compareTo)
                .orElse(null);
        if (latest != null && !latest.isBefore(lastTradingDay)) {
            log.debug("Candles for {} fresh (latest {}), skipping sync", ticker, latest);
            return;
        }
        try {
            technicalAnalysisUseCase.computeIndicators(ticker, lookbackDays);
            log.info("Technical readiness: synced candles for {}", ticker);
        } catch (Exception e) {
            log.warn("Technical readiness sync failed for {}: {}", ticker, e.getMessage());
        }
    }

    /** Most recent completed weekday (approximation; ignores exchange holidays). */
    private LocalDate lastTradingDay(LocalDate today) {
        LocalDate d = today;
        if (d.getDayOfWeek() == DayOfWeek.SATURDAY) {
            d = d.minusDays(1);
        } else if (d.getDayOfWeek() == DayOfWeek.SUNDAY) {
            d = d.minusDays(2);
        }
        return d;
    }

    @Override
    public TechnicalData fetchForTicker(String ticker) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(90);
        var candles = priceCandleRepository.findByTickerAndDateBetween(ticker, from, to);
        if (candles.isEmpty()) {
            return TechnicalData.empty(ticker);
        }

        // Build indicator map from candles: latest close, 14-day SMA
        Map<String, Double> indicators = new HashMap<>();
        int n = candles.size();
        double latestClose = candles.get(n - 1).close().doubleValue();
        indicators.put("CLOSE", latestClose);

        if (n >= 14) {
            double sma14 = candles.subList(n - 14, n).stream()
                    .mapToDouble(c -> c.close().doubleValue()).average().orElse(0);
            indicators.put("SMA_14", sma14);
        }

        String summary = "Latest close: %.4f. Based on %d daily candles from %s to %s."
                .formatted(latestClose, n, from, to);

        return new TechnicalData(ticker, indicators, summary);
    }
}
