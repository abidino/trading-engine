package com.trading.orchestration.infrastructure;

import com.trading.marketdata.domain.model.TrendAnalysis;
import com.trading.marketdata.domain.model.TrendDirection;
import com.trading.marketdata.domain.port.out.TrendAnalysisRepository;
import com.trading.orchestration.domain.port.out.TechnicalTrendPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Anticorruption adapter: translates the marketdata module's persisted trend
 * verdicts into a compact textual summary for the orchestration LLM prompt.
 */
@Component
@RequiredArgsConstructor
public class TechnicalTrendAdapter implements TechnicalTrendPort {

    private static final int WINDOW = 50;

    private final TrendAnalysisRepository trendRepository;

    @Override
    public String fetchTrendSummary(String ticker) {
        List<TrendAnalysis> recent = trendRepository.findRecentByTicker(ticker.toUpperCase(), WINDOW);
        if (recent.isEmpty()) {
            return "No technical-trend history recorded yet.";
        }

        TrendAnalysis latest = recent.get(0);

        Map<TrendDirection, Integer> counts = new EnumMap<>(TrendDirection.class);
        for (TrendAnalysis t : recent) {
            counts.merge(t.trend(), 1, Integer::sum);
        }

        StringBuilder distribution = new StringBuilder();
        for (TrendDirection d : TrendDirection.values()) {
            int c = counts.getOrDefault(d, 0);
            if (c > 0) {
                if (!distribution.isEmpty()) {
                    distribution.append(", ");
                }
                distribution.append(d).append("=").append(c);
            }
        }

        return """
                Latest verdict: %s (confidence %.2f) on %s.
                Over the last %d daily analyses: %s.
                Latest reasoning: %s
                """.formatted(
                latest.trend(), latest.confidence(), latest.analysisDate(),
                recent.size(), distribution, latest.reasoning()).trim();
    }
}
