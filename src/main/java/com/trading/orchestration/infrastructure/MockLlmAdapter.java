package com.trading.orchestration.infrastructure;

import com.trading.shared.kernel.AnalysisRequestType;
import com.trading.shared.kernel.llm.LlmPort;
import com.trading.shared.kernel.llm.LlmRequest;
import com.trading.shared.kernel.llm.LlmResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Deterministic, offline stand-in for {@link LlmPort} used by the {@code mock}
 * Spring profile. Returns canned, schema-correct JSON (or plain text) based on
 * the {@link AnalysisRequestType} and prompt content, so every module that calls
 * the LLM works end-to-end without a real provider (Ollama/OpenAI/Anthropic).
 */
@Slf4j
@Primary
@Profile("mock")
@Component
public class MockLlmAdapter implements LlmPort {

    @Override
    public LlmResponse complete(LlmRequest request) {
        String content = switch (request.analysisType()) {
            case DISCOVERY -> """
                    {"recommend":true,"confidence":0.72,"reasoning":"Mock: technicals constructive, no red flags.","trend":"UPTREND"}""";
            case TECHNICAL_TREND -> """
                    {"trend":"UPTREND","confidence":0.68,"reasoning":"Mock: short EMAs above long EMAs, RSI ~55, MACD positive."}""";
            case NEWS_SUMMARY -> newsContent(request);
            case SOCIAL_SUMMARY -> "Mock social summary: chatter is light and leans NEUTRAL.";
            case PORTFOLIO_REVIEW -> analysis("HOLD");
            case WATCHLIST_REVIEW -> analysis("WAIT");
        };
        log.debug("MockLlmAdapter returning canned response for {}", request.analysisType());
        return new LlmResponse(content, "mock-llm", 0, Duration.ZERO);
    }

    private String newsContent(LlmRequest request) {
        // The intelligence classification prompt asks for a "category" JSON;
        // the orchestration summariser asks for plain prose.
        if (request.systemPrompt() != null && request.systemPrompt().contains("category")) {
            boolean macro = !request.systemPrompt().contains("marketWide");
            return macro
                    ? """
                    {"category":"MACRO_ECONOMIC","sentiment":"NEUTRAL","summary":"Mock macro summary.","interpretation":"Mock: broadly neutral for the market."}"""
                    : """
                    {"category":"STOCK","sentiment":"POSITIVE","summary":"Mock article summary.","interpretation":"Mock: mildly positive for the stock.","marketWide":false}""";
        }
        return "Mock news summary: overall sentiment NEUTRAL; no single dominant driver.";
    }

    private String analysis(String action) {
        return ("""
                {"action":"%s","confidence":0.60,"reasoning":"Mock: balanced signals, staying patient.",""" +
                """
                "technicalSummary":"Mock technicals.","fundamentalSummary":"Mock fundamentals.",""" +
                """
                "newsSummary":"Mock news.","socialSummary":"Mock social."}""").formatted(action);
    }
}
