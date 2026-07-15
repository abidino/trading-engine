package com.trading.discovery.domain.service;

import com.trading.marketdata.domain.model.TechnicalIndicatorSnapshot;
import com.trading.shared.kernel.AnalysisRequestType;
import com.trading.shared.kernel.llm.LlmRequest;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Pure domain service: builds the LLM prompt that decides whether a freshly
 * screened candidate is worth recommending to the user — based ONLY on its
 * technical posture plus the screener's fundamental snapshot. No news/social
 * (discovery runs daily on unknown tickers where richer context isn't available).
 */
public class DiscoveryEvaluationPromptAssembler {

    private static final String SYSTEM_PROMPT = """
            You are a disciplined equity screener assistant. A stock passed a quantitative
            screen and you are given its latest technical indicators plus the screener's
            fundamental snapshot. Decide whether this stock is worth RECOMMENDING to the
            user as a potential watchlist candidate.

            Judge primarily on technical posture (moving-average alignment EMA9/20/50/100/200,
            RSI, MACD) confirming a constructive or improving trend, sanity-checked against the
            fundamentals provided. Be selective: only recommend genuinely promising setups.

            Respond ONLY with valid JSON (no markdown, no prose):
            {"recommend":true|false,"confidence":0.0,"reasoning":"...","trend":"STRONG_UPTREND|UPTREND|SIDEWAYS|DOWNTREND|STRONG_DOWNTREND"}
            confidence is between 0 and 1.
            """;

    public LlmRequest assemble(String ticker, Map<String, String> screenerCriteria,
                               TechnicalIndicatorSnapshot s) {
        String fundamentals = screenerCriteria == null || screenerCriteria.isEmpty()
                ? "(none)"
                : screenerCriteria.entrySet().stream()
                    .map(e -> "- " + e.getKey() + ": " + e.getValue())
                    .collect(Collectors.joining("\n"));

        String user = """
                ## Candidate: %s

                ## Screener Fundamentals
                %s

                ## Latest Technical Indicators (as of %s, %d candles)
                - Close: %s
                - EMA 9/20/50/100/200: %s / %s / %s / %s / %s
                - RSI(14): %s
                - MACD line/signal/histogram: %s / %s / %s
                """.formatted(
                ticker,
                fundamentals,
                s.asOfDate(), s.dataPoints(),
                s.close(),
                fmt(s.ema9()), fmt(s.ema20()), fmt(s.ema50()), fmt(s.ema100()), fmt(s.ema200()),
                fmt(s.rsi14()),
                fmt(s.macd()), fmt(s.macdSignal()), fmt(s.macdHistogram()));

        return LlmRequest.of(SYSTEM_PROMPT, user, AnalysisRequestType.DISCOVERY);
    }

    private String fmt(Double v) {
        return v == null ? "n/a" : String.valueOf(v);
    }
}
