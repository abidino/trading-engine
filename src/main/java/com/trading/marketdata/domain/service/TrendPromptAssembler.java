package com.trading.marketdata.domain.service;

import com.trading.marketdata.domain.model.TechnicalIndicatorSnapshot;
import com.trading.marketdata.domain.model.TrendAnalysis;
import com.trading.shared.kernel.AnalysisRequestType;
import com.trading.shared.kernel.llm.LlmRequest;

import java.util.List;

/**
 * Pure domain service: builds the LLM prompt that turns a computed
 * {@link TechnicalIndicatorSnapshot} (plus recent trend history) into a request
 * for a structured trend verdict. No Spring, no IO.
 */
public class TrendPromptAssembler {

    private static final String SYSTEM_PROMPT = """
            You are an expert technical analyst. You are given the latest computed technical
            indicators for a single stock plus a short history of previous daily trend verdicts.

            Classify the CURRENT trend into exactly one of:
              STRONG_UPTREND, UPTREND, SIDEWAYS, DOWNTREND, STRONG_DOWNTREND

            Guidelines:
            - Strong trends require clear moving-average alignment (e.g. price > EMA20 > EMA50 > EMA200
              for an uptrend, reversed for a downtrend) reinforced by MACD and RSI.
            - SIDEWAYS when moving averages are intertwined / flat and momentum is neutral.
            - Treat null indicators as "not enough history" and weight the available ones.
            - Consider the recent verdict history for continuity, but judge the latest data first.

            Respond ONLY with valid JSON (no markdown, no prose) matching this exact schema:
            {"trend":"STRONG_UPTREND|UPTREND|SIDEWAYS|DOWNTREND|STRONG_DOWNTREND","confidence":0.0,"reasoning":"..."}
            confidence is a number between 0 and 1.
            """;

    public LlmRequest assemble(TechnicalIndicatorSnapshot s, List<TrendAnalysis> recentHistory) {
        String user = """
                ## Stock: %s
                As of: %s  (candles used: %d)

                ## Latest Technical Indicators
                - Close: %s
                - EMA 9:   %s
                - EMA 20:  %s
                - EMA 50:  %s
                - EMA 100: %s
                - EMA 200: %s
                - RSI(14): %s
                - MACD line:      %s
                - MACD signal:    %s
                - MACD histogram: %s

                ## Recent Daily Trend Verdicts (most recent first)
                %s
                """.formatted(
                s.ticker(),
                s.asOfDate(),
                s.dataPoints(),
                s.close(),
                fmt(s.ema9()), fmt(s.ema20()), fmt(s.ema50()), fmt(s.ema100()), fmt(s.ema200()),
                fmt(s.rsi14()),
                fmt(s.macd()), fmt(s.macdSignal()), fmt(s.macdHistogram()),
                history(recentHistory)
        );
        return LlmRequest.of(SYSTEM_PROMPT, user, AnalysisRequestType.TECHNICAL_TREND);
    }

    private String history(List<TrendAnalysis> recent) {
        if (recent == null || recent.isEmpty()) {
            return "(no previous verdicts)";
        }
        StringBuilder sb = new StringBuilder();
        for (TrendAnalysis t : recent) {
            sb.append("- ").append(t.analysisDate())
                    .append(": ").append(t.trend())
                    .append(" (conf ").append(String.format("%.2f", t.confidence())).append(")\n");
        }
        return sb.toString().trim();
    }

    private String fmt(Double v) {
        return v == null ? "n/a" : String.valueOf(v);
    }
}
