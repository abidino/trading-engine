package com.trading.orchestration.infrastructure;

import com.trading.marketdata.domain.model.SupportResistanceLevels;
import com.trading.marketdata.domain.port.in.TechnicalAnalysisUseCase;
import com.trading.orchestration.domain.port.out.SupportResistancePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Anticorruption adapter: exposes the marketdata module's support/resistance calculation
 * to the orchestration domain as a compact {@link Snapshot}, degrading gracefully to
 * {@code null} when there is not enough price history.
 */
@Slf4j
@Component
public class SupportResistanceAdapter implements SupportResistancePort {

    private final TechnicalAnalysisUseCase technicalAnalysis;
    private final int lookbackDays;

    public SupportResistanceAdapter(
            TechnicalAnalysisUseCase technicalAnalysis,
            @Value("${orchestration.support-resistance.lookback-days:180}") int lookbackDays) {
        this.technicalAnalysis = technicalAnalysis;
        this.lookbackDays = lookbackDays;
    }

    @Override
    public Snapshot fetchForTicker(String ticker) {
        try {
            SupportResistanceLevels sr = technicalAnalysis.supportResistance(ticker, lookbackDays);
            return new Snapshot(
                    sr.close(),
                    sr.nearestSupport(),
                    sr.nearestResistance(),
                    sr.supports(),
                    sr.resistances());
        } catch (Exception e) {
            log.warn("Support/resistance unavailable for {}: {}", ticker, e.getMessage());
            return null;
        }
    }
}
